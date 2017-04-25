package ncei.catalog.amqp

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Producer {

  @Autowired
  private ProducerConfig taskProducerConfiguration

  void sendNewTask(ResponseMessage taskMessage) {
    taskProducerConfiguration.rabbitTemplate()
        .convertAndSend(taskProducerConfiguration.queueName, taskMessage)
  }
}
