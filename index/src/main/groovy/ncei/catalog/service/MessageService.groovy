package ncei.catalog.service

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class MessageService {

  @Autowired
  private Service service

  /**
   * Via Spring magic this handleMessage is recognized by the spring rabbit plugin.
   * @param message
   */
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
      log.warn("Insert task - received bad message: '$message'")
    }
  }
}
