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
  protected final String queueName = 'index-consumer'

  @Autowired
  private ResultHandler scrapingResultHandler

  @Bean
  Queue listenerQueue() {
    new Queue(this.queueName, true, false, true)
  }

  @Bean
  RabbitTemplate rabbitTemplate() {
    RabbitTemplate template = new RabbitTemplate(connectionFactory())
    template.setRoutingKey(this.queueName)
    template.setQueue(this.queueName)
    template.setMessageConverter(jsonMessageConverter())
    template
  }

  @Bean
  SimpleMessageListenerContainer listenerContainer() {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer()
    container.setConnectionFactory(connectionFactory())
    container.setQueueNames(this.queueName)
    container.setMessageListener(messageListenerAdapter())
    container
  }

  @Bean
  MessageListenerAdapter messageListenerAdapter() {
    new MessageListenerAdapter(scrapingResultHandler, jsonMessageConverter())
  }
}

