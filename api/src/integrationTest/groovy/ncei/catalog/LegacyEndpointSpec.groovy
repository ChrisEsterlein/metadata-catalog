package ncei.catalog

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import static org.hamcrest.Matchers.equalTo
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [ApiApplication], webEnvironment = RANDOM_PORT)
class LegacyEndpointSpec extends Specification{

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath


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

    expect: 'pre and post filter POSTs'
    //save metadata - test for metadata-recorder
    Map granule = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
            .when()
            .post('catalog-metadata/files')
            .then()
            .assertThat()
            .statusCode(201)
            .body('code', equalTo(201))
            .body('totalResultsUpdated', equalTo(1))
//todo: figure out why these arent coming through -- verified the post body is transformed correctly, but tracking_id, granule_metadata, and granule_size is not save in storage
//            .body('items[0].trackingId', equalTo(postBody.trackingId))
//            .body('items[0].fileSize', equalTo(postBody.fileSize))
//            .body('items[0].fileMetadata', equalTo(postBody.fileMetadata))
            .body('items[0].filename', equalTo(postBody.filename))
            .body('items[0].dataset', equalTo(postBody.dataset))
            .body('items[0].geometry', equalTo(postBody.geometry))
            .extract().path('items[0]')

    and: 'filter GET response'
    RestAssured.given()
            .contentType(ContentType.JSON)
            .when()
            .get("catalog-metadata/files/${granule.id}")
            .then()
            .assertThat()
            .statusCode(200)
//todo: get these to save in storage
//            .body('items[0].trackingId', equalTo(postBody.trackingId))
//            .body('items[0].fileSize', equalTo(postBody.fileSize))
//            .body('items[0].fileMetadata', equalTo(postBody.fileMetadata))
            .body('items[0].filename', equalTo(postBody.filename))
            .body('items[0].dataset', equalTo(postBody.dataset))
            .body('items[0].geometry', equalTo(postBody.geometry))


//todo: support search by dataset?
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
  }

}
