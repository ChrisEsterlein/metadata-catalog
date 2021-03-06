package org.cedar.metadata.index.service

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class MessageService {

  private Service service

  @Autowired
  MessageService(Service service) {
    this.service = service
  }

  /**
   * Via Spring magic this handleMessage is recognized by the spring rabbit plugin.
   * @param message
   */
  void handleMessage(Map message) {
    try {
      log.debug "Received rabbit message: $message"
      processResources(message)
    }
    catch(e) {
      log.error("Processing payload failed:  metadata=$message Exception:", e)
    }
  }

  /**
   * Handle a JSON-API style payload with an array of
   * resources needing to be either upserted or deleted
   * @param payload The payload to process. Must contain a "data"
   *                key which is an array of resources to process.
   * @return A JSON-API style response indicating the results
   */
  Map processResources(Map payload) {
    def resources = payload?.data
    if (resources instanceof Map) {
      resources = [resources]
    }
    if (!(resources instanceof List)) {
      log.warn("Received malformed payload of resources to process. Must contain data: ${payload}")
      return null
    }

    def created = 0
    def updated = 0
    def deleted = 0
    def failed = 0

    resources.each { resource ->
      log.debug("Begin update for metadata ${resource}")
      def result = null
      def action = resource?.meta?.action
      try {
        switch (action) {
          case 'insert':
          case 'update':
          case 'upsert':
            log.debug("Upserting metadata with id ${resource?.id}")
            result = service.upsert(resource)
            break

          case 'delete':
            log.debug("Deleting metadata with id ${resource?.id}")
            result = service.delete(resource)
            break

          default:
            log.warn("Received resource with unexpected action: $action")
        }
      }
      catch (e) {
        log.warn("An error occurred performing action [$action] on metadata with id ${resource?.id}", e)
      }

      if (result?.meta?.created == true) {
        created++
      }
      else if (result?.meta?.created == false) {
        updated++
      }
      else if (result?.meta?.deleted == true) {
        deleted++
      }
      else {
        failed++
      }
    }

    log.info("Handled update payload - created: $created, updated: $updated, deleted: $deleted, failed: $failed")
    return [meta: [created: created, updated: updated, deleted: deleted, failed: failed]]
  }
}
