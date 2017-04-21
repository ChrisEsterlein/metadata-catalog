package ncei.catalog.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static io.restassured.matcher.RestAssuredMatchers.*
import static org.hamcrest.Matchers.*


@Unroll
@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class MetadataRecorderApiSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath

    println 'hello'
  }

  def 'saves metadata posted by the ingest metadata recorder'() {
    def postBody = [
        trackingId  : 'ABCD',
        filename    : 'myfile',
        dataset     : 'test',
        fileSize    : 42,
        geometry    : 'POLYGON((0 0) (0 1) (1 1) (1 0))',
        fileMetadata: 'this is some raw scraped metadata from a header or whatever'
    ]

    expect:
    RestAssured.given()
        .body(postBody)
        .contentType(ContentType.JSON)
    .when()
        .post('/files')
    .then()
        .assertThat()
        .statusCode(500)
        .body('trackingId', equalTo(postBody.trackingId))
        .body('filename', equalTo(postBody.filename))
        .body('fileSize', equalTo(postBody.fileSize))
        .body('fileMetadata', equalTo(postBody.fileMetadata))
  }

}
