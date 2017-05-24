package ncei.catalog.service

import groovy.util.logging.Slf4j
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class MessageService {

  @Autowired
  RabbitTemplate rabbitTemplate

  void notifyIndex(Map details) {
    try{
      log.info "Notifying index of action"
      rabbitTemplate.convertAndSend('index-consumer', details)
    }catch(e){
      log.error('Failed to notify index with exception: ', e)
    }
  }
}
