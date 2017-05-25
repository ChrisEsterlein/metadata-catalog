package ncei.catalog.service

import groovy.util.logging.Slf4j
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Slf4j
@Component
class MessageService {

  @Autowired
  RabbitTemplate rabbitTemplate

  @Value ('${rabbitmq.routingKey}')
  String routingKey

  void notifyIndex(Map details) {
    try{
      log.info "Notifying index of action"
      log.debug "Notifying index: Rabbit routingKey=$routingKey details=$details"
      rabbitTemplate.convertAndSend(routingKey, details)
    }catch(e){
      log.error('Failed to notify index with exception: ', e)
    }
  }
}
