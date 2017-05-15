package ncei.catalog.controller

import ncei.catalog.Application
import ncei.catalog.config.RabbitConfig
import ncei.catalog.service.IndexAdminService
import ncei.catalog.service.Service
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@Unroll
@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class IndexRabbitApiSpec extends Specification {

  @Autowired
  RabbitConfig rabbitConfig
  @Autowired
  RabbitTemplate rabbitTemplate
  @Autowired
  Service service
  @Autowired
  IndexAdminService indexAdminService

  def poller = new PollingConditions(timeout: 5)

  def setup() {
    service.INDEX = 'test_index'
    if (indexAdminService.indexExists('test_index')) {
      indexAdminService.deleteIndex('test_index')
    }
    indexAdminService.createIndex('test_index')
  }

  def 'save to elastic search works'() {
    setup:
    Map metadata = [
        type      : 'junk',
        id        : '1',
        attributes: [
            dataset : 'testDataset',
            fileName: "testFileName"
        ]
    ]

    when:
    rabbitTemplate.convertAndSend(rabbitConfig.queueName, metadata)

    then:
    poller.eventually {
      def searchResults = service.search("fileName:${metadata.attributes.fileName}")
      assert searchResults.totalResults == 1
      assert searchResults.data[0] == metadata
    }
  }

  def 'malformed messages are handled gracefully'() {
    when:
    rabbitTemplate.convertAndSend(rabbitConfig.queueName, 'totes not json')
    sleep(1000)

    then:
    service.search().data.size() == 0
    rabbitTemplate.receive(rabbitConfig.queueName) == null
  }
}
