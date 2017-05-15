package ncei.catalog.service

import groovy.util.logging.Slf4j
import org.elasticsearch.client.Response
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@org.springframework.stereotype.Service
class IndexAdminService {

  protected RestClient restClient

  @Autowired
  IndexAdminService(RestClient restClient) {
    this.restClient = restClient
  }

  /**
   * Delete the specified index.
   * @return Json String indicating success or error
   * @throws org.elasticsearch.client.ResponseException If the index doesn't exist; I.E. Not found.
   */
  boolean deleteIndex(String indexName) throws ResponseException {
    log.debug("Delete Index: $indexName")
    Response response = restClient.performRequest("DELETE", "/$indexName")
    log.debug("Delete response: $response")

    return response.statusLine.statusCode == 200
  }

  /**
   * Does the index exist?
   * @return Boolean true if it exists, false otherwise.
   */
  boolean indexExists(String indexName) {
    log.debug("Index Exists: $indexName")
    Response response = restClient.performRequest("HEAD", "/$indexName")
    log.debug("Index Exists response: $response")

    return response.statusLine.statusCode == 200
  }

  /**
   * Create the index if it doesn't exist.  If it exists then it will fail to create the index.
   * @return Boolean true if it was created, false if there was an error creating it.
   */
  boolean createIndex(String indexName) {
    log.debug("Creating index: $indexName")
    Response response = restClient.performRequest("PUT", "/$indexName")
    log.debug("Creating index response: $response")

    return response.statusLine.statusCode == 200
  }

}
