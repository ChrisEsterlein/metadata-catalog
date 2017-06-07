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
class LegacyEndpointSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  String metadata_recorder_post_endpoint = '/metadata-catalog/granules/'
  String catalog_etl_get_endpoint = '/metadata-catalog/search/'

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def 'metadata-recorder POST gets expected response and etl is able to search for results via paging'() {
    setup:
    def poller = new PollingConditions(timeout: 5)
    def postBody0 = [
        trackingId  : 'ABCD',
        filename    : 'myfile',
        dataset     : 'LegacyDataset',
        fileSize    : 42,
        geometry    : 'POLYGON((0 0) (0 1) (1 1) (1 0))',
        fileMetadata: 'this is some raw scraped metadata from a header or whatever'
    ]

    def postBody2 = [
        trackingId  : 'ABCD',
        filename    : 'myfile',
        dataset     : 'LegacyDataset',
        fileSize    : 42,
        geometry    : 'POLYGON((0 0) (0 1) (1 1) (1 0))',
        fileMetadata: 'this is some raw scraped metadata from a header or whatever'
    ]

    Map pagingParams0 = [dataset: "${postBody0.dataset}", offset: "0", max: "1"]
    Map pagingParams1 = [dataset: "${postBody0.dataset}", offset: "1", max: "1"]

    expect: 'metadata-recorder saves 1 result and gets expected response'
    RestAssured.given()
        .body(postBody0)
        .contentType(ContentType.JSON)
        .when()
        .post(metadata_recorder_post_endpoint)
        .then()
        .assertThat()
        .statusCode(201)
        .body('code', equalTo(201))
        .body('totalResultsUpdated', equalTo(1))
        .body('items.size', equalTo(1))
        .body('items[0].trackingId', equalTo(postBody0.trackingId))
        .body('items[0].filename', equalTo(postBody0.filename))
        .body('items[0].dataset', equalTo(postBody0.dataset))
        .body('items[0].fileSize', equalTo(postBody0.fileSize))
        .body('items[0].geometry', equalTo(postBody0.geometry))
        .body('items[0].fileMetadata', equalTo(postBody0.fileMetadata))
        .extract().path('items[0]')

    and: 'etl can search for the 1 saved result and response is expected format'
    poller.eventually {
      RestAssured.given()
          .contentType(ContentType.JSON)
          .params(pagingParams0)
          .when()
          .get(catalog_etl_get_endpoint)
          .then()
          .assertThat()
          .statusCode(200)
          .body('items[0].trackingId', equalTo(postBody0.trackingId))
          .body('items[0].fileSize', equalTo(postBody0.fileSize))
          .body('items[0].geometry', equalTo(postBody0.geometry))
          .body('items[0].fileMetadata', equalTo(postBody0.fileMetadata))
          .body('items[0].filename', equalTo(postBody0.filename))
          .body('items[0].dataset', equalTo(postBody0.dataset))
    }

    and: 'metadata-recorder saves a second result'
    RestAssured.given()
        .body(postBody2)
        .contentType(ContentType.JSON)
        .when()
        .post(metadata_recorder_post_endpoint)
        .then()
        .assertThat()
        .statusCode(201)

    poller.eventually {
      and: 'etl searches with params dataset, offset, and max and response is expected format'
      Map responsePage0 = RestAssured.given()
          .contentType(ContentType.JSON)
          .params(pagingParams0)
          .when()
          .get(catalog_etl_get_endpoint)
          .then()
          .assertThat()
          .statusCode(200)
          .extract().response().as(Map)

      Map responsePage1 = RestAssured.given()
          .contentType(ContentType.JSON)
          .params(pagingParams1)
          .when()
          .get(catalog_etl_get_endpoint)
          .then()
          .assertThat()
          .statusCode(200)
          .extract().response().as(Map)

      and: 'verify max param: the two retrieved pages are expected size'
      responsePage0.items.size == 1
      responsePage1.items.size == 1

      and: 'verify offset param: the results should be different between the two different pages'
      responsePage0 != responsePage1
    }
  }
}

