package ncei.catalog

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasItems
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [ApiApplication], webEnvironment = RANDOM_PORT)
class UpdateDataAPISpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  def poller = new PollingConditions(timeout: 5)

  def postBody = [
          "tracking_id"     : "abc123",
          "filename"        : "granuleFace",
          "granule_schema"  : "a granule schema",
          "granule_size"    : 1024,
          "geometry"        : "POLYGON()",
          "access_protocol" : "FILE",
          "type"            : "fos",
          "granule_metadata": "{blah:blah}",
          "collections"     : ["a", "list", "of", "collections"]
  ]

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def 'update granules'() {
    setup: 'save some data'
    Map granuleMetadata = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
            .when()
            .post("storage1/granules")
            .then()
            .assertThat()
            .statusCode(201)
            .extract()
            .path('data[0].attributes')

    assert granuleMetadata.tracking_id == postBody.tracking_id
    String updatedFilename = 'a different filename'
    granuleMetadata.filename = updatedFilename

    when: 'we update the granule'
    RestAssured.given()
            .body(granuleMetadata)
            .contentType(ContentType.JSON)
            .when()
            .put("storage1/granules/$granuleMetadata.id")
            .then()
            .assertThat()
            .statusCode(200)
            .body('data[0].id', equalTo(granuleMetadata.id))
            .body('data[0].attributes.filename', equalTo(updatedFilename))

    then: 'we can get the updated record from storage'
    RestAssured.given()
            .body(granuleMetadata)
            .contentType(ContentType.JSON)
            .when()
            .get("storage1/granules/$granuleMetadata.id")
            .then()
            .assertThat()
            .statusCode(200)
            .body('data[0].id', equalTo(granuleMetadata.id))
            .body('data[0].attributes.filename', equalTo(updatedFilename))

    and: 'index got the update'
    poller.eventually {
      RestAssured.given()
              .contentType(ContentType.JSON)
              .params([q: "_id:$granuleMetadata.id"])
              .when()
              .get("index/search")
              .then()
              .assertThat()
              .statusCode(200)
              .body("data.size", equalTo(1))
              .body("data.id", hasItems(granuleMetadata.id))
    }

  }
}
