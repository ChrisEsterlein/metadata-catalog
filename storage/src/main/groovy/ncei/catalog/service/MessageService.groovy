package ncei.catalog.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
class MessageService {

  @Autowired
  RabbitTemplate rabbitTemplate

  void notifyIndex(String action, String type, String id, Map updatedRecord){
    Map jsonMessage = buildJsonApiSpec(action, type, id, updatedRecord)
    rabbitTemplate.convertAndSend('index-consumer', jsonMessage)
  }

  Map buildJsonApiSpec(String action, String type, String id, Map updatedRecord){
    [data: [[type: type, id:  id, attributes: updatedRecord]] , meta:[action: action]]
  }

}
