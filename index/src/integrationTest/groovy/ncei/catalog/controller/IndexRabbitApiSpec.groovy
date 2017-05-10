package ncei.catalog.controller

import ncei.catalog.Application
import ncei.catalog.amqp.ConsumerConfig
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
  RabbitTemplate rabbitTemplate

  @Autowired
  Service service

  def setup() {
    service.INDEX = 'test_index'
    if (service.indexExists()) { service.deleteIndex() }
    service.createIndex()
  }

  def 'rabbit save to elastic search works'() {
    setup:
    Map metadata = [type:'junk',
                    id: '1',
                    dataset: 'testDataset',
                    fileName: "testFileName"]

    def conditions = new PollingConditions(timeout: 20, initialDelay: 1.5, factor: 1.25)

    when:
    rabbitTemplate.convertAndSend(ConsumerConfig.queueName, metadata)

    then:
    conditions.eventually {
      def searchResults = service.search([q: "dataset:${metadata.dataset} fileName:${metadata.fileName}" as String])
      assert searchResults.totalResults == 1
      assert searchResults.data[0] == metadata
    }
  }
}
