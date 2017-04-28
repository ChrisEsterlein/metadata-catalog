package ncei.catalog.controller

import ncei.catalog.Application
import ncei.catalog.amqp.ConsumerConfig
import ncei.catalog.amqp.ConsumerMessage
import ncei.catalog.model.Metadata
import ncei.catalog.repository.MetadataRepository
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
  MetadataRepository repository

  @Autowired
  RabbitTemplate rabbitTemplate

  void setup() {
    repository.deleteAll()
  }

  def 'rabbit save to elastic search works'() {
    setup:
    def metadata = new Metadata(id: '1', dataset: 'testDataset', fileName: 'testFileName')
    ConsumerMessage message = new ConsumerMessage()
    message.setTask('save')
    message.setMetadata(metadata)

    def conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)

    when:
    rabbitTemplate.convertAndSend(ConsumerConfig.queueName, message)

    then:
    conditions.eventually {
      assert repository.findAll().size() == 1
      assert repository.findAll()[0] == metadata
    }
  }
}