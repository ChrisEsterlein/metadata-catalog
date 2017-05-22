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

  @Autowired RabbitConfig rabbitConfig
  @Autowired RabbitTemplate rabbitTemplate
  @Autowired Service service
  @Autowired IndexAdminService indexAdminService

  def poller = new PollingConditions(timeout: 5)

  def setup() {
    service.INDEX = 'test_index'
    if (indexAdminService.indexExists('test_index')) {
      indexAdminService.deleteIndex('test_index')
    }
    indexAdminService.createIndex('test_index')
  }

  def 'save one to elastic search'() {
    setup:
    Map message = [
        data: [id: 'abc', type: 'granule', attributes: [name: 'one'], meta: [action: 'insert']]
    ]

    when:
    rabbitTemplate.convertAndSend(rabbitConfig.queueName, message)

    then:
    poller.eventually {
      def searchResults = service.search([q:"name:one"])
      assert searchResults.meta.totalResults == 1
      assert searchResults.data[0] == message.data.subMap(['id', 'type', 'attributes'])
    }
  }

  def 'save multiple to elastic search'() {
    setup:
    Map message = [
        data: [
            [id: 'a', type: 'granule', attributes: [name: 'one'], meta: [action: 'insert']],
            [id: 'b', type: 'granule', attributes: [name: 'two'], meta: [action: 'update']],
        ]
    ]

    when:
    rabbitTemplate.convertAndSend(rabbitConfig.queueName, message)

    then:
    poller.eventually {
      assert service.search().data.size() == 2
    }
  }

  def 'save multiple to elastic search then delete one'() {
    setup:
    Map insertMessage = [
        data: [
            [id: 'a', type: 'granule', attributes: [name: 'one'], meta: [action: 'insert']],
            [id: 'b', type: 'granule', attributes: [name: 'two'], meta: [action: 'update']],
        ]
    ]
    Map deleteMessage = [
        data: [
            [id: 'a', type: 'granule', meta: [action: 'delete']],
        ]
    ]

    when:
    rabbitTemplate.convertAndSend(rabbitConfig.queueName, insertMessage)

    then:
    poller.eventually {
      assert service.search().data.size() == 2
    }

    when:
    rabbitTemplate.convertAndSend(rabbitConfig.queueName, deleteMessage)

    then:
    poller.eventually {
      assert service.search().data.size() == 1
      assert service.search().data[0].id == 'b'
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
