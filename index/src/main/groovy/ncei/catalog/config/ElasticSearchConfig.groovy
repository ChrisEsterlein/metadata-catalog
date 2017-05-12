package ncei.catalog.config

import groovy.util.logging.Slf4j
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Slf4j
@Configuration
class ElasticSearchConfig {

  @Value('${elasticsearch.host}')
  private String ELASTICSEARCH_HOST

  @Value('${elasticsearch.port}')
  private int ELASTICSEARCH_PORT

  @Bean
  RestClient restClient() {
    log.info("Building Elasticsearch connection: host=$ELASTICSEARCH_HOST port=$ELASTICSEARCH_PORT")
    return RestClient.builder(new HttpHost(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT)).build()
  }

}
