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
  def conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath

    service.INDEX = 'test_index'
    service.indexExists()? service.deleteIndex() : ''
    service.createIndex()
  }

  def 'Search for metadata with no search params'() {
    setup: 'Load data into elasticsearch for searching'
    Map metadata = [type:"junk",
                    dataset: "testDataset",
                    fileName: "testFileName1"]
    Map metadata2 = [type:"junk",
                     dataset: "testDataset",
                     fileName: "testFileName2"]
    def saved = service.insert(metadata)
    def saved2 = service.insert(metadata2)

    def expResult = metadata.clone()
    def expResult2 = metadata2.clone()
    // Add the id to saved metadata since appears back when you search.
    expResult.put("id", saved.data._id)
    expResult2.put("id", saved2.data._id)

    when: "Inserted data has appeared in the database"
    Map metadataSearch = generateElasticsearchQuery(metadata)
    Map metadataSearch2 = generateElasticsearchQuery(metadata2)

    conditions.eventually {
      def search = service.search(metadataSearch)
      def search2 = service.search(metadataSearch2)
      assert search.data.size() == 1
      assert search2.data.size() == 1
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
        .body("data.findAll{true}", hasItems(expResult, expResult2))
  }

  def 'Search for metadata with search params'() {
    setup: 'Load data into elasticsearch for searching'
    Map metadata = [type: "junk",
                    dataset: "testDataset",
                    fileName: "testFileName"]
    service.insert([type    : "junk",
                    dataset : "testDataset",
                    fileName: "testFileName2"])
    service.insert(metadata)

    when: "Inserted data has appeared in the database"
    Map metadataSearch = generateElasticsearchQuery(metadata)
    conditions.eventually {
      assert service.search(metadataSearch).data.size() == 1
    }

    then: "You can hit the application search endpoint WITH search params and get back the saved data"
    RestAssured.given()
        .contentType(contentType)
        .params(metadataSearch)
      .when()
        .get(SEARCH_ENDPOINT)
        .then()
      .assertThat()
        .statusCode(200)
        .body("data.size", equalTo(1))
        .body('data[0].dataset', equalTo(metadata.dataset))
        .body('data[0].fileName', equalTo(metadata.fileName))
  }

  /**
   * Convert map of parameters into an AND separated String and put into map with key "q"
   * @param params Map<String, String> params to search
   * @return Map ["q": "key:value pairs separated with AND"]
   */
  Map generateElasticsearchQuery(Map params) {
    String paramsConversion = ""
    Iterator it = params.entrySet().iterator()
    while (it.hasNext()) {
      Map.Entry<String, String> entry = it.next()
      paramsConversion += "${entry.getKey()}:${entry.getValue()}"
      it.hasNext()? paramsConversion += " AND " : ""
    }
    ["q": paramsConversion]
  }
}