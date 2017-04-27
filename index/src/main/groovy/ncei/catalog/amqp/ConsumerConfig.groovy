package ncei.catalog.amqp

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Configuration

@Configuration
class ConsumerConfig extends RabbitConfig {
  final static String queueName = 'index-consumer'

  @Autowired
  private ConsumerHandler scrapingResultHandler

  @Bean
  Queue listenerQueue() {
    new Queue(queueName, true, false, true)
  }

  @Bean
  RabbitTemplate rabbitTemplate() {
    RabbitTemplate template = new RabbitTemplate(connectionFactory())
    template.setRoutingKey(queueName)
    template.setQueue(queueName)
    template.setMessageConverter(jsonMessageConverter())
    template
  }

  @Bean
  SimpleMessageListenerContainer listenerContainer() {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer()
    container.setConnectionFactory(connectionFactory())
    container.setQueueNames(queueName)
    container.setMessageListener(messageListenerAdapter())
    container
  }

  @Bean
  MessageListenerAdapter messageListenerAdapter() {
    new MessageListenerAdapter(scrapingResultHandler, jsonMessageConverter())
  }
}

