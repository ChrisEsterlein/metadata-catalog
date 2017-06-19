package org.cedar.metadata.storage.config

import org.springframework.amqp.core.Queue
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile


@Profile("test")
@Configuration
class TestRabbitConfig{

  @Value('${rabbitmq.queue}')
  String queueName

  @Bean
  Queue indexQueue() {
    new Queue(queueName, true, false, false)
  }

}
