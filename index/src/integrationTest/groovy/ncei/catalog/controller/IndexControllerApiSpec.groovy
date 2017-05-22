package ncei.catalog.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import ncei.catalog.service.IndexAdminService
import ncei.catalog.service.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasItems
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class IndexControllerApiSpec extends Specification {
  final String SEARCH_ENDPOINT = '/search'

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  @Autowired
  Service service
  @Autowired
  IndexAdminService indexAdminService

  private ContentType contentType = ContentType.JSON
  def poller = new PollingConditions(timeout: 5)

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath

    service.INDEX = 'test_index'
    if (indexAdminService.indexExists('test_index')) {
      indexAdminService.deleteIndex('test_index')
    }
    indexAdminService.createIndex('test_index')
  }

  def 'Search for metadata with no search params'() {
    setup: 'Load data into elasticsearch for searching'
    Map metadata = [
        id        : '1',
        type      : "junk",
        attributes: [dataset: "testDataset", fileName: "testFileName1"]
    ]
    Map metadata2 = [
        id        : '2',
        type      : "junk",
        attributes: [dataset: "testDataset", fileName: "testFileName2"]
    ]

    when: "Inserted data has appeared in elasticsearch"
    service.upsert(metadata)
    service.upsert(metadata2)
    poller.eventually {
      assert service.search().data.size() == 2
    }

    then: "You can hit the application search endpoint WITHOUT search params and get back the saved data"
    RestAssured.given()
        .contentType(contentType)
      .when()
        .get(SEARCH_ENDPOINT)
        .then()
        .assertThat()
      .statusCode(200)
        .body("data.size", equalTo(2))
        .body("data.id", hasItems(metadata.id, metadata2.id))
  }

  def 'Search for metadata with search params'() {
    setup: 'Load data into elasticsearch for searching'
    Map metadata = [
        id        : '1',
        type      : "junk",
        attributes: [dataset: "testDataset", fileName: "testFileName1"]
    ]
    Map metadata2 = [
        id        : '2',
        type      : "junk",
        attributes: [dataset: "testDataset", fileName: "testFileName2"]
    ]
    service.upsert(metadata)
    service.upsert(metadata2)

    when: "Inserted data has appeared in the database"
    def metadataSearch = generateQueryString(metadata.attributes)
    poller.eventually {
      assert service.search().data.size() == 2
    }

    then: "You can hit the application search endpoint WITH search params and get back the saved data"
    RestAssured.given()
        .contentType(contentType)
        .params([q: metadataSearch])
      .when()
        .get(SEARCH_ENDPOINT)
        .then()
      .assertThat()
        .statusCode(200)
        .body("data.size", equalTo(1))
        .body('data[0].dataset', equalTo(metadata.dataset))
        .body('data[0].fileName', equalTo(metadata.fileName))
  }

  def 'returns empty list when nothing has been indexed'() {
    expect:
    RestAssured.given()
        .contentType(contentType)
      .when()
        .get(SEARCH_ENDPOINT)
      .then()
        .assertThat()
        .statusCode(200)
        .body("data.size", equalTo(0))
  }

  /**
   * Convert map of parameters into an AND separated String
   * @param params Map<String, String> params to search
   * @return String with key:value pairs separated with AND
   */
  String generateQueryString(Map params) {
    params.collect({ k, v -> "$k:$v" }).join(' AND ')
  }
}
