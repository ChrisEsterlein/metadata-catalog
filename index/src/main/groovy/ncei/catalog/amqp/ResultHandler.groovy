package ncei.catalog.amqp

import groovy.util.logging.Slf4j
import ncei.catalog.repository.MetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class ResultHandler {

  @Autowired
  private MetadataRepository metadataRepository

  void handleMessage(ConsumerMessage consumerMessage) {
    System.out.println("Received rabbit message: $consumerMessage")

    String task = consumerMessage.task?.toLowerCase()
    switch (task) {
      case 'save':
        consumerMessage.metadata?
            metadataRepository.save(consumerMessage.metadata) :
            log.info ("Save task: not saving metadata '$consumerMessage.metadata'")
        break
      default:
        log.info "Unknown task to execute '$consumerMessage.task'"
    }
  }
}
