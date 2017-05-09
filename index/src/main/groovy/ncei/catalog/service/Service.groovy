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
@org.springframework.stereotype.Service/**/
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
  def search(Map searchQuery) {
    String endpoint = "/$INDEX/_search"

    log.debug("Search: endpoint=$endpoint query=$searchQuery")
    def response = restClient.performRequest("GET", endpoint, searchQuery)
    log.debug("Search response: $response")

    int statusCode = response.getStatusLine().getStatusCode()
    String bodyStr = EntityUtils.toString(response.getEntity())
    def result = new JsonSlurper().parseText(bodyStr)

    return [
        data       : result.hits.hits.collect({
          Map map = [type: it._type, id: it._id]
          map.putAll((Map) it._source)
          map
        }),
        totalResults: result.hits.total,
        searchTerms : searchQuery,
        code        : statusCode
    ]
  }

  /**
   * Insert the metadata to Elasticsearch
   * @param metadata data to insert
   * @return Json String of inserted entry or error
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

    return response
  }

  /**
   * Delete the specified index.
   * @return Json String indicating success or error
   * @throws ResponseException If the index doesn't exist; I.E. Not found.
   */
  def deleteIndex() throws ResponseException {
    String endpoint = "/$INDEX"

    log.debug("Delete Index: endpoint=$endpoint")
    Response response = restClient.performRequest("DELETE", endpoint)
    log.debug("Delete response: $response")
    int statusCode = response.getStatusLine().getStatusCode()

    return [
        message: "SUCCESS: Deleted index $INDEX",
        code: statusCode]
  }

  /**
   * Does the index exist?
   * @return Boolean true if it exists; false otherwise
   */
  boolean indexExists() {
    String endpoint = "/$INDEX?"
    log.debug("Index Exists: endpoint=$endpoint")
    Response response = restClient.performRequest("HEAD", endpoint)
    int statusCode = response.getStatusLine().getStatusCode()

    return statusCode == 200
  }

  /**
   * Create the index
   * @return Response indicates if the index was created or not (which is usually due to it already existing).
   */
  boolean createIndex() {
    String endpoint = "/$INDEX?"
    log.debug("Creating index: endpoint=$endpoint")
    Response response = restClient.performRequest("PUT", endpoint)

    return response
  }
}