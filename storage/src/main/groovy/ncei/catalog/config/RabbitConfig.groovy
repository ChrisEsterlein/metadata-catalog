package ncei.catalog.config

import ncei.catalog.service.MessageService
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.DefaultClassMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

  @Value('${rabbitmq.connectionfactory.virtualHost}')
  String virtualHost

  @Value('${rabbitmq.connectionfactory.host}')
  String host

  @Value('${rabbitmq.connectionfactory.username}')
  String username

  @Value('${rabbitmq.connectionfactory.password}')
  String password

  @Value('${rabbitmq.queue}')
  String queueName

  @Autowired
  MessageService messageService

  @Bean
  ConnectionFactory connectionFactory() {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host)
    connectionFactory.setVirtualHost(virtualHost)
    connectionFactory.setUsername(username)
    connectionFactory.setPassword(password)
    connectionFactory
  }

  @Bean
  AmqpAdmin amqpAdmin() {
    new RabbitAdmin(connectionFactory())
  }

  @Bean
  MessageConverter jsonMessageConverter() {
    final Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter()
    converter.setClassMapper(classMapper())
    converter
  }

  @Bean
  DefaultClassMapper classMapper() {
    DefaultClassMapper typeMapper = new DefaultClassMapper()
    typeMapper.setDefaultType(Map.class)
    typeMapper
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
  Queue indexQueue() {
    new Queue(queueName, true, false, true)
  }

  @Bean
  MessageListenerAdapter messageListenerAdapter() {
    new MessageListenerAdapter(messageService, jsonMessageConverter())
  }

}