package ncei.catalog.service

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.entity.ContentType
import org.apache.http.nio.entity.NStringEntity
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.Response
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value

import javax.annotation.PostConstruct

@Slf4j
@org.springframework.stereotype.Service
class Service {

  @Value('${elasticsearch.host}')
  private String ELASTICSEARCH_HOST

  @Value('${elasticsearch.port}')
  private int ELASTICSEARCH_PORT

  protected RestClient restClient

  private String INDEX = 'search_index'
  private String GRANULE_TYPE = 'metadata'

  @PostConstruct
  protected buildClients() {
    log.info("Setting Elasticsearch connection: host=$ELASTICSEARCH_HOST port=$ELASTICSEARCH_PORT")

    restClient = RestClient.builder(new HttpHost(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT)).build()
    indexExists()?: createIndex()
  }

  /**
   * Search elasticsearch with query that is passed in.
   * @param searchQuery the query to execute against elasticsearch (Ex: [q:dataset:csb fileName:name1] )
   * @return Map of items
   */
  def search(Map<String, String> searchQuery) {
    String endpoint = "/$INDEX/_search"

    log.debug("Search: endpoint=$endpoint query=$searchQuery")
    def response = restClient.performRequest("GET", endpoint, searchQuery)
    log.debug("Search response: $response")

    def result = parseResponse(response)

    return [
        data        : result.hits.hits.collect({
          Map map = [type: it._type, id: it._id]
          map.putAll((Map) it._source)
          map
        }),
        totalResults: result.hits.total,
        searchTerms : searchQuery,
        code        : result.statusCode
    ]
  }

  /**
   * Insert the metadata to Elasticsearch
   * @param metadata data to insert
   * @return The inserted item
   */
  def insert(Map metadata) {
    String endpoint = "/$INDEX/$GRANULE_TYPE"

    log.debug("Insert: endpoint=$endpoint metadata=$metadata")
    String metadataStr = JsonOutput.toJson(metadata)
    HttpEntity entity = new NStringEntity(metadataStr, ContentType.APPLICATION_JSON)
    log.debug("Insert entity=${entity.toString()}")
    Response response = restClient.performRequest(
        "POST", // POST for _id optional
        endpoint,
        Collections.<String, String>emptyMap(),
        entity)
    log.debug("Insert response: $response")

    def result = parseResponse(response)

    return [
        data: [
            id: result._id,
            type: result._type,
            attributes: [
              created: result.created
            ]]
        ]
  }

  /**
   * Delete the specified index.
   * @return Json String indicating success or error
   * @throws ResponseException If the index doesn't exist; I.E. Not found.
   */
  boolean deleteIndex() throws ResponseException {
    String endpoint = "/$INDEX"

    log.debug("Delete Index: endpoint=$endpoint")
    Response response = restClient.performRequest("DELETE", endpoint)
    log.debug("Delete response: $response")

    def result = parseResponse(response)

    return result
  }

  /**
   * Does the index exist?
   * @return Boolean true if it exists, false otherwise.
   */
  protected boolean indexExists() {
    String endpoint = "/$INDEX?"
    log.debug("Index Exists: endpoint=$endpoint")
    Response response = restClient.performRequest("HEAD", endpoint)

    return response
  }

  /**
   * Create the index if it doesn't exist.  If it exists then it will fail to create the index.
   * @return Boolean true if it was created, false if there was an error creating it.
   */
  protected boolean createIndex() {
    String endpoint = "/$INDEX?"
    log.debug("Creating index: endpoint=$endpoint")
    Response response = restClient.performRequest("PUT", endpoint)

    return response
  }

  def parseResponse (Response response) {
    String body = response?.getEntity()? EntityUtils.toString(response?.getEntity()) : null
    Map result = [:]
    try {
      body? result = new JsonSlurper().parseText(body) : ''
    } catch (e) {
      log.info("Failed JsonSlurper() on body=$body", e)
    }

    result.put('statusCode', response.getStatusLine().getStatusCode())

    return result
  }
}