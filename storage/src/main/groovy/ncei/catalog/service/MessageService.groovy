package ncei.catalog.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
class MessageService {

  @Autowired
  RabbitTemplate rabbitTemplate

  void notifyIndex(Map details){
    rabbitTemplate.convertAndSend('index-consumer', details)
  }

  def handleMessage(Map message){
    return message
  }
}
