package ncei.catalog

import groovy.json.JsonOutput
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

  static final String METADATA_RECORDER_POST_ENDPOINT = '/metadata-catalog/granules/'
  static final String CATALOG_ETL_GET_ENDPOINT = '/metadata-catalog/search/'

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def 'HAPPY PATH: metadata-recorder POST gets expected response and etl is able to search for results via paging'() {
    setup:
    def poller = new PollingConditions(timeout: 5)
    def fileUri = 'FILE:///tmp/one.tif'
    def fileUriParts = fileUri.split('://')

    def postBody0 = [
        dataset        : 'LegacyDataset',
        trackingId     : 'ABCD',
        filename       : 'myfile.txt',
        fileSize       : 42,
        geometry       : 'POLYGON((0 0) (0 1) (1 1) (1 0))',
        fileMetadata   : JsonOutput.toJson([platform: 'blah', ship: 'whatever']),
        access_protocol: fileUriParts[0], //TODO: This is temporary until we have the module that will populate this
        file_path      : fileUriParts[1] //TODO: This is temporary until we have the  module that will populate this
    ]
    def postBody1 = postBody0
    postBody1.filename = 'different_than_other_saved_record.txt'

    Map pagingParams0 = [dataset: "${postBody0.dataset}", offset: "0", max: "1"]
    Map pagingParams1 = [dataset: "${postBody0.dataset}", offset: "1", max: "1"]
    Map expSearchTerms0 = [q: "dataset:${pagingParams0.dataset}", from: "0", size: "1"]

    expect: 'SAVE: metadata-recorder saves 1 result and gets expected response'
    RestAssured.given()
        .body(postBody0)
        .contentType(ContentType.JSON)
        .when()
        .post(METADATA_RECORDER_POST_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(201)
        .body('code', equalTo(201))
        .body('totalResultsUpdated', equalTo(1))
        .body('items[0].dataset', equalTo(postBody0.dataset))
        .body('items[0].trackingId', equalTo(postBody0.trackingId))
        .body('items[0].filename', equalTo(postBody0.filename))
        .body('items[0].fileSize', equalTo(postBody0.fileSize))
        .body('items[0].geometry', equalTo(postBody0.geometry))
        .body('items[0].fileMetadata', equalTo(postBody0.fileMetadata))
        .body('items[0].accessProtocol', equalTo(postBody0.access_protocol))
        .body('items[0].filePath', equalTo(postBody0.file_path))

    and: 'SEARCH: etl can search for the 1 saved result and the response is formatted properly'
    Map responsePage0
    poller.eventually {
      responsePage0 = RestAssured.given()
          .contentType(ContentType.JSON)
          .params(pagingParams0)
          .when()
          .get(CATALOG_ETL_GET_ENDPOINT)
          .then()
          .assertThat()
          .statusCode(200)
          .extract().response().as(Map)

      // Because of the way polling fails doing an assert instead of a check in the RestAssured call gives clarity of what failed.
      assert responsePage0.dataset == postBody0.dataset
      assert responsePage0.items[0].trackingId == postBody0.trackingId
      assert responsePage0.items[0].filename == postBody0.filename
      assert responsePage0.items[0].fileSize == postBody0.fileSize
      assert responsePage0.items[0].accessProtocol == fileUriParts[0]
      assert responsePage0.items[0].filePath == fileUriParts[1]
      assert responsePage0.items[0].geometry == postBody0.geometry
      assert responsePage0.items[0].fileMetadata == postBody0.fileMetadata
      assert responsePage0.items.size == 1
      assert responsePage0.totalResults == 1
      assert responsePage0.searchTerms == expSearchTerms0
      assert responsePage0.code == 200
    }

    and: 'metadata-recorder saves a second result'
    RestAssured.given()
        .body(postBody1)
        .contentType(ContentType.JSON)
        .when()
        .post(METADATA_RECORDER_POST_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(201)

    and: 'PAGING: etl searches with params dataset, offset, and max and response is expected format'
    poller.eventually {
      Map responsePage1 = RestAssured.given()
          .contentType(ContentType.JSON)
          .params(pagingParams1)
          .when()
          .get(CATALOG_ETL_GET_ENDPOINT)
          .then()
          .assertThat()
          .statusCode(200)
          .extract().response().as(Map)

      and: 'verify max param: the two retrieved pages are expected size'
      assert responsePage0.items.size == 1
      assert responsePage1.items.size == 1

      and: 'verify offset param: the results should be different between the two different pages'
      assert responsePage0 != responsePage1
    }
  }
}

