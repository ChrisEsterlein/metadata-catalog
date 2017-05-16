package ncei.catalog.config

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.exceptions.NoHostAvailableException
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.convert.CassandraConverter
import org.springframework.data.cassandra.convert.MappingCassandraConverter
import org.springframework.data.cassandra.core.CassandraAdminOperations
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext
import org.springframework.data.cassandra.mapping.CassandraMappingContext
import org.springframework.data.cassandra.mapping.SimpleUserTypeResolver
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable

@Configuration
@ConfigurationProperties(prefix = 'cassandra')
@EnableCassandraRepositories(basePackages = ["ncei.catalog.domain"])
class CassandraConfig {

  //-- Set by @ConfigurationProperties
  String keyspace
  String contactPoints
  Integer port
  //---


  @Bean
  @Retryable(value = NoHostAvailableException, maxAttempts = 12, backoff = @Backoff(delay = 100L, maxDelay = 500L))
  CassandraClusterFactoryBean cluster() {
    verifyConnection()

    def cluster = new CassandraClusterFactoryBean()
    cluster.setContactPoints(contactPoints)
    cluster.setPort(port)
    cluster.setReconnectionPolicy(new ExponentialReconnectionPolicy(500, 32000))

    return cluster
  }

  @Bean
  CassandraMappingContext mappingContext() {
    def mappingContext = new BasicCassandraMappingContext()
    mappingContext.setUserTypeResolver(new SimpleUserTypeResolver(cluster().getObject(), keyspace))

    return mappingContext
  }

  @Bean
  CassandraConverter converter() {
    return new MappingCassandraConverter(mappingContext())
  }

  @Bean
  CassandraSessionFactoryBean session() throws Exception {
    def session = new CassandraSessionFactoryBean()
    session.setCluster(cluster().getObject())
    session.setKeyspaceName(keyspace)
    session.setConverter(converter())
    session.setSchemaAction(SchemaAction.NONE)

    return session
  }

  @Bean
  CassandraOperations cassandraTemplate() throws Exception {
    return new CassandraTemplate(session().getObject())
  }

  @Bean
  CassandraAdminOperations cassandraAdminTemplate() throws Exception {
    return new CassandraAdminTemplate(session().getObject(), new MappingCassandraConverter())
  }

  private void verifyConnection() {
    def builder = Cluster.builder()
    contactPoints.split(',').each {
      builder.addContactPointsWithPorts(new InetSocketAddress(InetAddress.getByName(it), port))
    }
    def cluster = builder.build()
    def exception = null
    def tries = 30
    while (tries > 0) {
      try {
        cluster.connect(keyspace)
        return
      }
      catch (NoHostAvailableException e) {
        exception = e
        tries--
        sleep(1000)
      }
    }
    throw exception ?: new IllegalStateException(
            "Failed to verify cassandra connection on port $port for contact points $contactPoints")
  }

}
