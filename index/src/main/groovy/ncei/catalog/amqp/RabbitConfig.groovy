package ncei.catalog.amqp

import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.support.converter.DefaultClassMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

  @Bean
  ConnectionFactory connectionFactory() {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost")
    connectionFactory.setUsername('guest')
    connectionFactory.setPassword('guest')
    connectionFactory.setPort(5672)
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
    typeMapper.setDefaultType(ConsumerMessage.class)
    typeMapper
  }
}