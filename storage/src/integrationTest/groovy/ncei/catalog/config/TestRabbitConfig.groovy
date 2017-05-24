package ncei.catalog.config

import ncei.catalog.service.MessageService
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile


@Profile("test")
@Configuration
class TestRabbitConfig{

  @Autowired
  MessageService messageService

  @Autowired
  MessageConverter jsonMessageConverter

  @Autowired
  ConnectionFactory connectionFactory

  @Value('${rabbitmq.queue}')
  String queueName

  @Bean
  Queue indexQueue() {
    new Queue(queueName, true, false, false)
  }

  @Bean
  MessageListenerAdapter messageListenerAdapter() {
    new MessageListenerAdapter(messageService, jsonMessageConverter)
  }

}