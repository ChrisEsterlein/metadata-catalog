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
      Map response = service.insert(message)
      if (response.containsKey('data')) {
        log.info "Insert succeeded: metadata='$message' with Elasticsearch response: $response"
      } else {
        log.info "Insert failed: metadata='$message' with Elasticsearch response: $response"
      }
    } else {
      log.error("Insert task: did not insert metadata='$message'")
    }
  }
}
