package ncei.catalog.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Ignore

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static io.restassured.matcher.RestAssuredMatchers.*
import static org.hamcrest.Matchers.*

@Unroll
@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class OldCatalogMetadataApiSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def 'test old interfaces for metadata-recorder and etl'() {
    def postBody = [
        trackingId  : 'ABCD',
        filename    : 'myfile',
        dataset     : 'test',
        fileSize    : 42,
        geometry    : 'POLYGON((0 0) (0 1) (1 1) (1 0))',
        fileMetadata: 'this is some raw scraped metadata from a header or whatever'
    ]

    expect:
    //save metadata - test for metadata-recorder
    RestAssured.given()
        .body(postBody)
        .contentType(ContentType.JSON)
    .when()
        .post('/files')
    .then()
        .assertThat()
        .statusCode(200)  //should be a 201
        .body('trackingId', equalTo(postBody.trackingId))
        .body('filename', equalTo(postBody.filename))
        .body('fileSize', equalTo(postBody.fileSize))
        .body('fileMetadata', equalTo(postBody.fileMetadata))
        .body('dataset', equalTo(postBody.dataset))
        .body('geometry', equalTo(postBody.geometry))

    //get it using old endpoint - test for catalog-etl
    RestAssured.given()
        .param('dataset', 'test')
        .param('start_time', (new Date() - 100))
    .when()
        .get('/files')
    .then()
        .assertThat()
        .statusCode(200)
        .body('items[0].trackingId', equalTo(postBody.trackingId))
        .body('items[0].filename', equalTo(postBody.filename))
        .body('items[0].fileSize', equalTo(postBody.fileSize))
        .body('items[0].fileMetadata', equalTo(postBody.fileMetadata))
        .body('items[0].dataset', equalTo(postBody.dataset))
        .body('items[0].geometry', equalTo(postBody.geometry))

    //get it back out using new endpoint so we can get the granule_id we need to delete it
    String response = RestAssured.given()
      .param('dataset', 'test')
    .when()
      .get('/granules')
    .then()
      .assertThat()
      .statusCode(200)
      .body('granules[0].filename', equalTo(postBody.filename))
      .body('granules[0].granule_size', equalTo(postBody.fileSize))
      .body('granules[0].granule_metadata', equalTo(postBody.fileMetadata))
      .body('granules[0].geometry', equalTo(postBody.geometry))
    .extract()
      .path('granules[0].granule_id' as String)

    def deleteBody = [granule_id: response as String]

    //delete it
    RestAssured.given()
            .body(deleteBody)
            .contentType(ContentType.JSON)
            .when()
            .delete('/delete')
            .then()
            .assertThat()
            .statusCode(200)
  }

  def 'update by tracking id'() {
    def postBody = [
            trackingId  : 'ABCD',
            filename    : 'myfile',
            dataset     : 'test',
            fileSize    : 42,
            geometry    : 'POLYGON((0 0) (0 1) (1 1) (1 0))',
            fileMetadata: 'this is some raw scraped metadata from a header or whatever'
    ]

    when: 'we post the same tracking id twice'
    //save metadata - test for metadata-recorder
    RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
          .when()
            .post('/files')
          .then()
            .assertThat()
            .statusCode(200)  //should be 201
            .body('trackingId', equalTo(postBody.trackingId))
            .body('filename', equalTo(postBody.filename))
            .body('fileSize', equalTo(postBody.fileSize))
            .body('fileMetadata', equalTo(postBody.fileMetadata))
            .body('dataset', equalTo(postBody.dataset))
            .body('geometry', equalTo(postBody.geometry))

    Map updatedPostBody = postBody.clone()
    String updatedMetadata = 'different metadata'
    updatedPostBody.fileMetadata = updatedMetadata

    RestAssured.given()
            .body(updatedPostBody)
            .contentType(ContentType.JSON)
          .when()
            .post('/files')
          .then()
            .assertThat()
            .statusCode(200)  //should be a 200
            .body('trackingId', equalTo(updatedPostBody.trackingId))
            .body('filename', equalTo(updatedPostBody.filename))
            .body('fileSize', equalTo(updatedPostBody.fileSize))
            .body('fileMetadata', equalTo(updatedPostBody.fileMetadata))
            .body('dataset', equalTo(updatedPostBody.dataset))
            .body('geometry', equalTo(updatedPostBody.geometry))

    then: 'we can see that we updated by granule_id'
    //get it using old endpoint - test for catalog-etl
    RestAssured.given()
            .param('dataset', 'test')
            .param('versions', true)
          .when()
            .get('/files')
          .then()
            .assertThat()
            .statusCode(200)
    //first on is the newest
            .body('items[0].trackingId', equalTo(updatedPostBody.trackingId))
            .body('items[0].filename', equalTo(updatedPostBody.filename))
            .body('items[0].fileSize', equalTo(updatedPostBody.fileSize))
            .body('items[0].fileMetadata', equalTo(updatedPostBody.fileMetadata))
            .body('items[0].dataset', equalTo(updatedPostBody.dataset))
            .body('items[0].geometry', equalTo(updatedPostBody.geometry))
    //second is the original
            .body('items[1].trackingId', equalTo(postBody.trackingId))
            .body('items[1].filename', equalTo(postBody.filename))
            .body('items[1].fileSize', equalTo(postBody.fileSize))
            .body('items[1].fileMetadata', equalTo(postBody.fileMetadata))
            .body('items[1].dataset', equalTo(postBody.dataset))
            .body('items[1].geometry', equalTo(postBody.geometry))

    //get it back out using new endpoint so we can get the granule_id we need to delete it
    String granule_id = RestAssured.given()
            .param('dataset', 'test')
          .when()
            .get('/granules')
          .then()
            .assertThat()
            .statusCode(200)
            .body('granules[0].filename', equalTo(updatedPostBody.filename))
            .body('granules[0].granule_size', equalTo(updatedPostBody.fileSize))
            .body('granules[0].granule_metadata', equalTo(updatedPostBody.fileMetadata))
            .body('granules[0].geometry', equalTo(updatedPostBody.geometry))
          .extract()
            .path('granules[0].granule_id' as String)

    def deleteBody = [granule_id: granule_id]

    //purge all it
    RestAssured.given()
            .body(deleteBody)
            .contentType(ContentType.JSON)
          .when()
            .delete('/granules/purge')
          .then()
            .assertThat()
            .statusCode(200)
            .body('message' as String, equalTo('Successfully purged 2 rows matching ' + deleteBody))

  }

}



