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
class LegacyEndpointSpec extends Specification{

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  def poller = new PollingConditions(timeout: 5)

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def postBody = [
          trackingId  : 'ABCD',
          filename    : 'myfile',
          dataset     : 'test',
          fileSize    : 42,
          geometry    : 'POLYGON((0 0) (0 1) (1 1) (1 0))',
          fileMetadata: 'this is some raw scraped metadata from a header or whatever'
  ]

  def 'test old interfaces for metadata-recorder and etl'() {

    expect:
    //save metadata - test for metadata-recorder
    RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
            .when()
            .post('catalog-metadata/files')
            .then()
            .assertThat()
            .statusCode(201)
//            .body('items[0].trackingId', equalTo(postBody.trackingId))
//            .body('items[0].fileSize', equalTo(postBody.fileSize))
//            .body('items[0].fileMetadata', equalTo(postBody.fileMetadata))
            .body('items[0].filename', equalTo(postBody.filename))
            .body('items[0].dataset', equalTo(postBody.dataset))
            .body('items[0].geometry', equalTo(postBody.geometry))

//    //get it using old endpoint - test for catalog-etl
//    RestAssured.given()
//            .param('dataset', 'test')
//            .when()
//            .get('catalog-metadata/files')
//            .then()
//            .assertThat()
//            .statusCode(200)
//            .body('items[0].trackingId', equalTo(postBody.trackingId))
//            .body('items[0].filename', equalTo(postBody.filename))
//            .body('items[0].fileSize', equalTo(postBody.fileSize))
//            .body('items[0].fileMetadata', equalTo(postBody.fileMetadata))
//            .body('items[0].dataset', equalTo(postBody.dataset))
//            .body('items[0].geometry', equalTo(postBody.geometry))

    //get it back out using new endpoint so we can get the id we need to delete it
//    String response = RestAssured.given()
//            .when()
//            .get('storage1/granules')
//            .then()
//            .assertThat()
//            .statusCode(200)
//            .body('data[0].attributes.filename', equalTo(postBody.filename))
//            .body('data[0].attributes.granule_size', equalTo(postBody.fileSize))
//            .body('data[0].attributes.granule_metadata', equalTo(postBody.fileMetadata))
//            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
//            .extract()
//            .path('granules[0].id' as String)
//
//    def deleteBody = [id: response as String]
//
//    //delete it
//    RestAssured.given()
//            .body(deleteBody)
//            .contentType(ContentType.JSON)
//            .when()
//            .delete('granules')
//            .then()
//            .assertThat()
//            .statusCode(200)
  }

}
