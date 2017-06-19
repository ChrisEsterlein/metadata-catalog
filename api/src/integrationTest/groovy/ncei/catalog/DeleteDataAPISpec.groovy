package ncei.catalog

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.hamcrest.Matchers.equalTo
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [ApiApplication], webEnvironment = RANDOM_PORT)
class DeleteDataAPISpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  static final String STORAGE_GRANULES_ENDPOINT = '/storage/granules'

  def poller = new PollingConditions(timeout: 5)

  def postBody = [
      "filename"        : "granuleFace",
      "metadata_schema"  : "a granule schema",
      "granule_size"    : 1024,
      "geometry"        : "POLYGON()",
      "access_protocol" : "FILE",
      "type"            : "fos",
      "metadata": "{blah:blah}",
      "collections"     : ["a", "list", "of", "collections"]
  ]

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def 'delete granules'() {
    setup: 'save some data'
    Map granuleMetadata = RestAssured.given()
        .body(postBody)
        .contentType(ContentType.JSON)
        .when()
        .post(STORAGE_GRANULES_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(201)
        .extract()
        .path('data[0].attributes')

    when: 'we delete the granule'
    RestAssured.given()
        .body(granuleMetadata)
        .contentType(ContentType.JSON)
        .when()
        .delete("$STORAGE_GRANULES_ENDPOINT/$granuleMetadata.id")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data[0].id', equalTo(granuleMetadata.id))
        .body('data[0].attributes.deleted', equalTo(true))

    then: 'it is deleted from storage'
    RestAssured.given()
        .body(granuleMetadata)
        .contentType(ContentType.JSON)
        .when()
        .get("$STORAGE_GRANULES_ENDPOINT/$granuleMetadata.id")
        .then()
        .assertThat()
        .statusCode(404)

    and: 'it is deleted in the index'
    poller.eventually {
      RestAssured.given()
          .contentType(ContentType.JSON)
          .params([q: "_id:$granuleMetadata.id"])
          .when()
          .get("index/search")
          .then()
          .assertThat()
          .statusCode(200)
          .body("data.size", equalTo(0))
    }
  }

}
