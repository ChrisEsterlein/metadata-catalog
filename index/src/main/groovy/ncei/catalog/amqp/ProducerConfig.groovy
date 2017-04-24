package ncei.catalog.amqp

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.amqp.core.Queue

@Configuration
class ProducerConfig extends RabbitConfig {

  protected final String queueName = 'index-producer'

  @Bean
  RabbitTemplate rabbitTemplate() {
    RabbitTemplate template = new RabbitTemplate(connectionFactory())
    template.setRoutingKey(this.queueName)
    template.setQueue(this.queueName)
    template.setMessageConverter(jsonMessageConverter())
    return template
  }

  @Bean
  Queue queue() {
    new Queue(this.queueName)
  }
}
