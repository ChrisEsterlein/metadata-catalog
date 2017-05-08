package ncei.catalog.service

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.springframework.beans.factory.annotation.Value

import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletResponse

@Slf4j
@org.springframework.stereotype.Service/**/
class Service {

  @Value('${elasticsearch.host}')
  private String ELASTICSEARCH_HOST

  @Value('${elasticsearch.port}')
  private String ELASTICSEARCH_PORT

  protected HTTPBuilder elasticsearchClient

//  @Value('${elasticsearch.index.prefix:}${elasticsearch.index.search.name}')
  private String INDEX = 'search_index'

//  @Value('${elasticsearch.index.search.granuleType}')
  private String GRANULE_TYPE = 'metadata'

  @PostConstruct
  protected buildClients() {
    def url = "http://$ELASTICSEARCH_HOST:$ELASTICSEARCH_PORT"
    elasticsearchClient = new HTTPBuilder("$url")
    log.info("Set Elasticsearch client url: ${elasticsearchClient.uri} from host=$ELASTICSEARCH_HOST port=$ELASTICSEARCH_PORT")
  }

  /**
   * Search elasticsearch with query that is passed in.
   * @param searchQuery the query to execute against elasticsearch (Ex: [q:dataset:csb fileName:name1] )
   * @return
   */
  def search(Map searchQuery) {
    String path = "/$INDEX/_search"

    log.info("Performing search query: uri: ${elasticsearchClient.uri} path: $path query: $searchQuery")
    def response = elasticsearchClient.get(path: path, query: searchQuery)
    def result = [
        items       : response.hits.hits.collect({
          Map map = [type: it._type, id: it._id]
          map.putAll((Map) it._source)
          map
        }),
        totalResults: response.hits.total,
        searchTerms : searchQuery,
        code        : HttpServletResponse.SC_OK
    ]
    log.debug("Search response: $response")
    result
  }

  /**
   * Save the metadata to Elasticsearch
   * @param metadata data to save
   * @return
   */
  def save(def metadata) {
    String path = "/$INDEX/$GRANULE_TYPE"

    log.info("Performing save: uri: ${elasticsearchClient.uri} path: $path metadata: $metadata")
    def response = elasticsearchClient.post(path: path, body: metadata, requestContentType: ContentType.JSON)
    log.debug("Save response: $response")
    response
  }

  /**
   * Delete the set index.
   * @return map with message and code
   */
  def deleteIndex() {
    String path = "/$INDEX"

    log.info("Performing delete: path: $path")
    def response = elasticsearchClient.request(Method.DELETE, ContentType.JSON) {
      uri.path = path
      requestContentType = ContentType.JSON

      response.success = { resp, json ->
        return [
            message: "SUCCESS: Deleted index $INDEX",
            code: resp.status
        ]
      }
      response.failure = { resp ->
        return [
            message: "FAILURE: $resp.responseBase",
            code: resp.status]
      }
    }
    log.debug("Delete response: $response")

    response
  }
}
