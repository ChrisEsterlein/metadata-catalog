package ncei.catalog.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import ncei.catalog.model.Metadata
import ncei.catalog.repository.MetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import static org.hamcrest.Matchers.equalTo
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class IndexControllerApiSpec extends Specification {
  final String SEARCH_ENDPOINT = '/search'

  @Autowired
  MetadataRepository repository

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  private ContentType contentType = ContentType.JSON

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath

    repository.deleteAll()
  }

  def 'Search for metadata with no params'() {
    setup: 'Load data into elasticsearch for searching'
    def metadata = new Metadata(id: '1', dataset: 'testDataset', fileName: 'testFileName')
    repository.save(metadata)

    expect:
    RestAssured.given()
        .contentType(contentType)
        .when()
        .get(SEARCH_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(200)
        .body('items[0].id', equalTo(metadata.id))
        .body('items[0].dataset', equalTo(metadata.dataset))
        .body('items[0].fileName', equalTo(metadata.fileName))
  }

  def 'Search for metadata with params'() {
    setup: 'Load data into elasticsearch for searching'
    def metadata = new Metadata(id: '1', dataset: 'testDataset', fileName: 'testFileName')
    repository.save(metadata)
    def metadata2 = new Metadata(id: '2', dataset: 'testDataset', fileName: 'testFileName2')
    repository.save(metadata2)

    Map query = [q: "dataset:${metadata.dataset} fileName:${metadata.fileName}"]

    expect:
    RestAssured.given()
        .contentType(contentType)
        .params(query)
        .when()
        .get(SEARCH_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(200)
        .body('items[0].dataset', equalTo(metadata.dataset))
        .body('items[0].dataset', equalTo(metadata.dataset))
  }
}



