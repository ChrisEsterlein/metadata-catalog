package ncei.catalog.amqp

import groovy.util.logging.Slf4j

import ncei.catalog.service.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class ConsumerHandler {

  @Autowired
  private Service service

  void handleMessage(Map message) {
    log.info "Received rabbit message: $message"

    if (message) {
      Map response = service.save(message)
      if (response.containsKey('_id') && response.containsKey('_index') && response.containsKey('_type')) {
        log.info "Save succeeded: metadata='$message' with Elasticsearch response: $response"
      } else {
        log.info "Save failed: metadata='$message' with Elasticsearch response: $response"
      }
    } else {
      log.error("Save task: did not save metadata='$message'")
    }
  }
}
