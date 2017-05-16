package ncei.catalog.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.convert.MappingCassandraConverter
import org.springframework.data.cassandra.core.CassandraAdminOperations
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories

@Configuration
@ConfigurationProperties(prefix = 'cassandra')
@EnableCassandraRepositories(basePackages = ["ncei.catalog.domain"])
class IntegrationCassandraConfig extends CassandraConfig {

  @Bean
  CassandraAdminOperations cassandraAdminTemplate() throws Exception {
    return new CassandraAdminTemplate(session().getObject(), new MappingCassandraConverter())
  }
}