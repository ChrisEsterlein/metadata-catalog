package ncei.catalog.amqp

import groovy.util.logging.Slf4j
import ncei.catalog.repository.MetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class ConsumerHandler {

  @Autowired
  private MetadataRepository metadataRepository

  void handleMessage(ConsumerMessage consumerMessage) {
    log.info "Received rabbit message: $consumerMessage"

    String task = consumerMessage.task?.toLowerCase()
    switch (task) {
      case 'save':
        if (consumerMessage.metadata) {
          metadataRepository.save(consumerMessage.metadata)
          log.info "Save task: saved metadata='$consumerMessage.metadata'"
        } else {
          log.error("Save task: did not save metadata='$consumerMessage.metadata'")
        }
        break
      default:
        log.info "Unknown task '$consumerMessage.task' to execute"
    }
  }
}
