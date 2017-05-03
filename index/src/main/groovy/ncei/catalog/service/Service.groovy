package ncei.catalog.service

import groovy.util.logging.Slf4j
import groovyx.net.http.HTTPBuilder
import org.springframework.beans.factory.annotation.Value

import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletResponse

@Slf4j
@org.springframework.stereotype.Service
class Service {

  @Value('${elasticsearch.hostName}')
  private String ELASTICSEARCH_HOSTNAME
  protected HTTPBuilder elasticsearchClient

//  @Value('${elasticsearch.index.prefix:}${elasticsearch.index.search.name}')
  private String SEARCH_INDEX = 'search_index'

//  @Value('${elasticsearch.index.search.granuleType}')
//  private String GRANULE_TYPE = 'metadata'

  @PostConstruct
  protected buildClients() {
    def url = ELASTICSEARCH_HOSTNAME
    log.info("Elasticsearch client url: $url")
    elasticsearchClient = new HTTPBuilder(url)
  }

  def search(def searchQuery) {
    String path = "/$SEARCH_INDEX/_search"

    log.info("Performing search query: uri: ${elasticsearchClient.uri} query: $searchQuery")
    def searchResponse = elasticsearchClient.get(path: path, query: searchQuery)
    def result = [
        items       : searchResponse.hits.hits.collect({
          Map map = [type: it._type, id: it._id]
          map.putAll((Map) it._source)
          map
        }),
        totalResults: searchResponse.hits.total,
        searchTerms : searchQuery,
        code        : HttpServletResponse.SC_OK
    ]
    result
  }
}
