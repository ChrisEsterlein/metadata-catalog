package ncei.catalog.controller

import ncei.catalog.Application
import ncei.catalog.amqp.ConsumerConfig
import ncei.catalog.amqp.ConsumerMessage
import ncei.catalog.model.Metadata
import ncei.catalog.repository.MetadataRepository
import org.mockito.MockitoAnnotations
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@Unroll
@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class IndexRabbitApiSpec extends Specification {

  @Autowired
  MetadataRepository repository

  @Autowired
  RabbitTemplate rabbitTemplate

  @Ignore
  def 'rabbit save to elastic search works' () {
    setup:
    ConsumerMessage message = new ConsumerMessage()
    message.setTask('save')
    message.setMetadata(new Metadata(id:'1', dataset: 'testDataset', fileName: 'testFileName'))

    when:
    rabbitTemplate.convertAndSend(ConsumerConfig.queueName, message)

    then:
    repository.save(message.metadata) == "{id:'1'}"
  }
}



