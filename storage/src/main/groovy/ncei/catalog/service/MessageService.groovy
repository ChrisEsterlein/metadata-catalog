package ncei.catalog.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
class MessageService {

  @Autowired
  RabbitTemplate rabbitTemplate

  void notifyIndex(Map updatedRecord){
    rabbitTemplate.convertAndSend('index-consumer', updatedRecord)
  }

}
