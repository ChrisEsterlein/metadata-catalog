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

  def poller = new PollingConditions(timeout: 5)

  def setup() {
    service.INDEX = 'test_index'
    if (service.indexExists()) { service.deleteIndex() }
    service.createIndex()
  }

  def 'rabbit save to elastic search works'() {
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
    rabbitTemplate.convertAndSend(ConsumerConfig.queueName, metadata)

    then:
    poller.eventually {
      def searchResults = service.search("fileName:${metadata.attributes.fileName}")
      assert searchResults.totalResults == 1
      assert searchResults.data[0] == metadata
    }
  }

  def 'malformed rabbit messages are handled gracefully'() {
    when:
    rabbitTemplate.convertAndSend(ConsumerConfig.queueName, 'totes not json')
    sleep(5000)

    then:
    service.search().data.size() == 0
    rabbitTemplate.receive(ConsumerConfig.queueName) == null
  }
}
