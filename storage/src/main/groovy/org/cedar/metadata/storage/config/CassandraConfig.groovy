package org.cedar.metadata.storage.config

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.exceptions.NoHostAvailableException
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy
import com.datastax.driver.core.policies.ReconnectionPolicy
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cassandra.config.CassandraCqlClusterFactoryBean
import org.springframework.cassandra.config.DataCenterReplication
import org.springframework.cassandra.core.keyspace.CreateKeyspaceSpecification
import org.springframework.cassandra.core.keyspace.KeyspaceOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.data.cassandra.config.CassandraEntityClassScanner
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration
import org.springframework.data.cassandra.convert.CassandraConverter
import org.springframework.data.cassandra.convert.MappingCassandraConverter
import org.springframework.data.cassandra.core.CassandraAdminOperations
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext
import org.springframework.data.cassandra.mapping.CassandraMappingContext
import org.springframework.data.cassandra.mapping.SimpleUserTypeResolver
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable

@Configuration
@Slf4j
@ConfigurationProperties(prefix = 'cassandra')
class CassandraConfig extends AbstractCassandraConfiguration {
 
  //-- Set by @ConfigurationProperties
  String keyspace
  String contactPoints
  Integer port
  Boolean init
  //---

  @Value(value = "classpath:createKeyspaceAndTables.cql")
  private Resource initDbScript

  @Bean
  @Retryable(value = NoHostAvailableException, maxAttempts = 12, backoff = @Backoff(delay = 100L, maxDelay = 500L))
  @Override
  CassandraCqlClusterFactoryBean cluster() {
    CassandraCqlClusterFactoryBean bean = super.cluster()
    //verifyConnection()
    return bean
  }

  @Override
  protected ReconnectionPolicy getReconnectionPolicy() {
    new ExponentialReconnectionPolicy(500, 32000)
  }

  @Override
  protected List<String> getStartupScripts() {
    List startUpScripts = initDbScript.getInputStream().text.trim().tokenize(';')
    log.debug "Startup CQL scripts: $startUpScripts"
    init ? startUpScripts : []
  }

  @Override
  SchemaAction getSchemaAction() {
    SchemaAction.CREATE_IF_NOT_EXISTS
  }

  @Override
  protected String getKeyspaceName() {
    keyspace
  }

  @Override
  protected int getPort() {
    port
  }

  @Override
  protected String getContactPoints() {
    contactPoints
  }

  @Override
  String[] getEntityBasePackages() {
    ['org.cedar.metadata']
  }

  @Bean
  CassandraMappingContext mappingContext() {
    CassandraMappingContext mappingContext = new BasicCassandraMappingContext()
    mappingContext.setUserTypeResolver(new SimpleUserTypeResolver(cluster().getObject(), keyspace))
    mappingContext
  }

  @Bean
  CassandraConverter converter() {
    new MappingCassandraConverter(mappingContext())
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
