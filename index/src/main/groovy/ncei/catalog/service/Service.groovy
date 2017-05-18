package ncei.catalog.service

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.nio.entity.NStringEntity
import org.elasticsearch.client.Response
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@org.springframework.stereotype.Service
class Service {

  static String INDEX = 'search_index'
  protected RestClient restClient
  protected IndexAdminService indexAdminService

  @Autowired
  Service(RestClient restClient, IndexAdminService indexAdminService) {
    this.restClient = restClient
    this.indexAdminService = indexAdminService
    if (!indexAdminService.indexExists(INDEX)) {
      indexAdminService.createIndex(INDEX)
    }
  }

  /**
   * Search elasticsearch with query that is passed in.
   * @param searchQuery the query to execute against elasticsearch (Ex: "dataset:csb fileName:name1" )
   * @return Map of items
   */
  Map search(String searchQuery = null) {
    String endpoint = "/$INDEX/_search"

    log.debug("Search: endpoint=$endpoint query=$searchQuery")
    def response = searchQuery ?
        restClient.performRequest('GET', endpoint, [q: searchQuery] as Map<String, String>) :
        restClient.performRequest('GET', endpoint)
    log.debug("Search response: $response")

    def result = parseResponse(response)
    return [
        data: result.hits.hits.collect({
          [id: it._id, type: it._type, attributes: it._source]
        }),
        meta: [
            totalResults: result.hits.total,
            searchTerms : searchQuery,
            code        : result.statusCode
        ]
    ]
  }

  /**
   * Handle a JSON-API style payload with an array of
   * resources needing to be either upserted or deleted
   * @param payload The payload to process. Must contain a "data"
   *                key which is an array of resources to process.
   * @return A JSON-API style response indicating the results
   */
  Map updateResources(Map payload) {
    def resources = payload?.data
    if (resources instanceof Map) {
      resources = [resources]
    }
    if (!(resources instanceof List)) {
      log.warn("Received malformed payload of resources to update. Must contain data: ${payload}")
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
            result = upsert(resource)
            break

          case 'delete':
            log.debug("Deleting metadata with id ${resource?.id}")
            result = delete(resource)
            break
        }
      }
      catch (e) {
        log.warn("An error occurred performing action [$action] on metadata with id ${resource?.id}", e)
      }

      if (result) {
        if (result?.meta?.created == true) {
          created++
        }
        if (result?.meta?.created == false) {
          updated++
        }
        if (result?.meta?.deleted == true) {
          deleted++
        }
      }
      else {
        failed++
      }
    }

    log.info("Handled update payload - created: $created, updated: $updated, deleted: $deleted, failed: $failed")
    return [meta: [created: created, updated: updated, deleted: deleted, failed: failed]]
  }

  /**
   * Insert a resource to Elasticsearch
   * @param resource data to upsert
   * @return The inserted resource
   */
  Map upsert(Map resource) {
    def id = resource?.id
    def type = resource?.type
    def attributes = resource?.attributes
    if (!(id instanceof String || id instanceof Number) || !(type instanceof String) || !(attributes instanceof Map)) {
      log.warn("Indexed objects must have an id, type, and attributes. Ignoring: ${resource}")
      return null
    }

    String endpoint = "/$INDEX/$type/$id"
    log.debug("Insert: endpoint=$endpoint resource=$resource")
    String metadataStr = JsonOutput.toJson(resource.attributes)
    HttpEntity entity = new NStringEntity(metadataStr, ContentType.APPLICATION_JSON)
    log.debug("Insert entity=${entity.toString()}")
    Response response = restClient.performRequest(
        'PUT',
        endpoint,
        Collections.<String, String> emptyMap(),
        entity)
    log.debug("Insert response: $response")

    def result = parseResponse(response)
    return [
        id        : result._id,
        type      : result._type,
        attributes: attributes,
        meta      : [created: result.created]
    ]
  }

  /**
   * Delete a resource from Elasticsearch
   * @param resource data to delete
   * @return The deleted resource with id, type, and whether it was deleted or not
   */
  Map delete(Map resource) {
    def id = resource?.id
    def type = resource?.type
    if (!(id instanceof String || id instanceof Number) || !(type instanceof String)) {
      log.warn("Can only delete resources with an id and type. Ignoring: ${resource}")
      return null
    }

    def result = [id: id, type: type, meta: [deleted: true]]
    try {
      String endpoint = "/$INDEX/$type/$id"
      log.debug("Delete: endpoint=$endpoint resource=$resource")
      String metadataStr = JsonOutput.toJson(resource.attributes)
      HttpEntity entity = new NStringEntity(metadataStr, ContentType.APPLICATION_JSON)
      log.debug("Delete entity=${entity.toString()}")
      Response response = restClient.performRequest('DELETE', endpoint)
      log.debug("Delete response: $response")

      def parsed = parseResponse(response)
      result.meta.deleted = (parsed.result == 'deleted')
    }
    catch (ResponseException e) {
      if (e.getResponse().statusLine.statusCode == 404) {
        result.meta.deleted = false
      }
      else {
        throw e
      }
    }

    return result
  }

  private static Map parseResponse(Response response) {
    Map result = [statusCode: response?.getStatusLine()?.getStatusCode() ?: 500]
    try {
      if (response?.getEntity()) {
        result += new JsonSlurper().parse(response?.getEntity()?.getContent()) as Map
      }
    }
    catch (e) {
      log.warn("Failed to parse elasticsearch response as json", e)
    }
    return result
  }
}
