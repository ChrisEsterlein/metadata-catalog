package ncei.catalog.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
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

  private ContentType contentType = ContentType.JSON

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath

    service.INDEX = 'test_index'
    service.deleteIndex()
  }

  def 'Search for metadata with no params'() {
    setup: 'Load data into elasticsearch for searching'
    Map metadata = [type:'junk',
                    dataset: 'testDataset',
                    fileName: "testFileName1"]
    Map metadata2 = [type:'junk',
                     dataset: 'testDataset',
                     fileName: "testFileName2"]
    def saved = service.save(metadata)
    def saved2 = service.save(metadata2)

    def expResult = metadata.clone()
    def expResult2 = metadata2.clone()
    // Add the id to saved metadata since appears back when you search.
    expResult.put("id", (String)saved._id)
    expResult2.put("id", (String)saved2._id)

    when:
    def conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)
    Map metadataSearch = generateElasticsearchQuery(metadata)
    Map metadataSearch2 = generateElasticsearchQuery(metadata2)
    conditions.eventually {
      assert service.search(metadataSearch).items.size() == 1
      assert service.search(metadataSearch2).items.size() == 1
    }

    then:
    RestAssured.given()
        .contentType(contentType)
      .when()
        .get(SEARCH_ENDPOINT)
        .then()
        .assertThat()
      .statusCode(200)
        .body("items.size", equalTo(2))
        .body("items.findAll{true}", hasItems(expResult, expResult2))
  }

  def 'Search for metadata with params'() {
    setup: 'Load data into elasticsearch for searching'
    Map metadata = [type:'junk',
                    dataset: 'testDataset',
                    fileName: "testFileName"]
    service.save(metadata)
    service.save([type:'junk',
                  dataset: 'testDataset',
                  fileName: "testFileName2"])

    when:
    def conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)
    Map metadataSearch = generateElasticsearchQuery(metadata)
    conditions.eventually {
      assert service.search(metadataSearch).items.size() == 1
    }

    then:
    RestAssured.given()
        .contentType(contentType)
        .params(metadataSearch)
      .when()
        .get(SEARCH_ENDPOINT)
        .then()
      .assertThat()
        .statusCode(200)
        .body("items.size", equalTo(1))
        .body('items.size()', equalTo(1))
        .body('items[0].dataset', equalTo(metadata.dataset))
        .body('items[0].fileName', equalTo(metadata.fileName))
  }

  Map generateElasticsearchQuery(Map params) {
    String key = params.toMapString().replaceAll(",", " AND")
    [q: "${key.substring(1, key.length()-1)}"]
  }
}