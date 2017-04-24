package ncei.catalog.amqp

import ncei.catalog.repository.MetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ResultHandler {

  @Autowired
  private MetadataRepository metadataRepository

  void handleMessage(ConsumerMessage scrapingResultMessage) {
    System.out.println("Received rabbit message: $scrapingResultMessage")
  }
}
