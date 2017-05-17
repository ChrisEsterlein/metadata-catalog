package ncei.catalog.service

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.nio.entity.NStringEntity
import org.elasticsearch.client.Response
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@org.springframework.stereotype.Service
class Service {

  static String INDEX = 'search_index'
  protected RestClient restClient
  protected IndexAdminService indexAdminService

  @Autowired
  Service(RestClient restClient, IndexAdminService indexAdminService) {
    this.restClient = restClient
    this.indexAdminService = indexAdminService
    if (!indexAdminService.indexExists(INDEX)) {
      indexAdminService.createIndex(INDEX)
    }
  }

  /**
   * Search elasticsearch with query that is passed in.
   * @param searchQuery the query to execute against elasticsearch (Ex: "dataset:csb fileName:name1" )
   * @return Map of items
   */
  Map search(String searchQuery = null) {
    String endpoint = "/$INDEX/_search"

    log.debug("Search: endpoint=$endpoint query=$searchQuery")
    def response = searchQuery ?
        restClient.performRequest('GET', endpoint, [q: searchQuery] as Map<String, String>) :
        restClient.performRequest('GET', endpoint)
    log.debug("Search response: $response")

    def result = parseResponse(response)
    return [
        data: result.hits.hits.collect({
          [id: it._id, type: it._type, attributes: it._source]
        }),
        meta: [
            totalResults: result.hits.total,
            searchTerms : searchQuery,
            code        : result.statusCode
        ]
    ]
  }

  /**
   * Insert the metadata to Elasticsearch
   * @param metadata data to insert
   * @return The inserted item
   */
  Map insert(Map metadata) {
    def id = metadata?.id
    def type = metadata?.type
    def attributes = metadata?.attributes
    if (!(id instanceof String || id instanceof Number) || !(type instanceof String) || !(attributes instanceof Map)) {
      log.warn("Indexed objects must have an id, type, and attributes. Ignoring: ${metadata}")
      return null
    }

    String endpoint = "/$INDEX/$type/$id"
    log.debug("Insert: endpoint=$endpoint metadata=$metadata")
    String metadataStr = JsonOutput.toJson(metadata.attributes)
    HttpEntity entity = new NStringEntity(metadataStr, ContentType.APPLICATION_JSON)
    log.debug("Insert entity=${entity.toString()}")
    Response response = restClient.performRequest(
        'PUT',
        endpoint,
        Collections.<String, String> emptyMap(),
        entity)
    log.debug("Insert response: $response")

    def result = parseResponse(response)
    return [
        data: [
            id        : result._id,
            type      : result._type,
            attributes: attributes,
            meta      : [created: result.created]
        ]
    ]
  }

  private static Map parseResponse(Response response) {
    Map result = [statusCode: response?.getStatusLine()?.getStatusCode() ?: 500]
    try {
      if (response?.getEntity()) {
        result += new JsonSlurper().parse(response?.getEntity()?.getContent()) as Map
      }
    }
    catch (e) {
      log.warn("Failed to parse elasticsearch response as json", e)
    }
    return result
  }
}
