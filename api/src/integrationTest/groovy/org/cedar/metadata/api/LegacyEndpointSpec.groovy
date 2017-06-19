package org.cedar.metadata.api

import groovy.json.JsonOutput
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [ApiApplication], webEnvironment = RANDOM_PORT)
class LegacyEndpointSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  static final String METADATA_RECORDER_POST_ENDPOINT = '/metadata-catalog/granules/'
  static final String CATALOG_ETL_GET_ENDPOINT = '/metadata-catalog/search/'

  def fileUri = 'FILE:///tmp/one.tif'
  def fileUriParts = fileUri.split('://')

  final def postBody = [
      dataset        : 'LegacyDataset',
      trackingId     : '7c25b383-2d7e-464f-81e9-84beed4c90ff',
      filename       : 'myfile.txt',
      fileSize       : 42,
      geometry       : 'POLYGON((0 0) (0 1) (1 1) (1 0))',
      fileMetadata   : JsonOutput.toJson([platform: 'blah', ship: 'whatever']),
      access_protocol: fileUriParts[0], //TODO: This is temporary until we have the module that will populate this
      file_path      : fileUriParts[1] //TODO: This is temporary until we have the  module that will populate this
  ]

  def poller = new PollingConditions(timeout: 5)

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def 'metadata-recorder performs POST and that works (does not care about response)'() {
    setup:

    expect: 'SAVE: metadata-recorder saves 1 result and gets unchanged response from DB representing what was Posted'
    RestAssured.given()
        .body(postBody)
        .contentType(ContentType.JSON)
        .when()
        .post(METADATA_RECORDER_POST_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(201)
  }

  def 'ETL performs GET with legacy paging params and gets expected response'() {
    setup:
    def postBody1 = postBody.clone()
    postBody1.trackingId = UUID.randomUUID().toString()
    def postBody2 = postBody.clone()
    postBody2.trackingId = UUID.randomUUID().toString()

    Map pagingParams0 = [dataset: "${postBody.dataset}", offset: "0", max: "1"]
    Map pagingParams1 = pagingParams0.clone()
    pagingParams1.offset = "1"

    Map expSearchTerms0 = [q: "dataset:${pagingParams0.dataset}", offset: pagingParams0.offset, max: pagingParams0.max, dataset: pagingParams0.dataset]

    when: 'metadata-recorder 2 results'
    RestAssured.given()
        .body(postBody1)
        .contentType(ContentType.JSON)
        .when()
        .post(METADATA_RECORDER_POST_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(201)
    RestAssured.given()
        .body(postBody2)
        .contentType(ContentType.JSON)
        .when()
        .post(METADATA_RECORDER_POST_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(201)

    then: 'etl can search for 1 saved result and the response is formatted properly'
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
      assert responsePage0.dataset == postBody.dataset
      assert responsePage0.items[0].filename == postBody.filename
      assert responsePage0.items[0].fileSize == postBody.fileSize
      assert responsePage0.items[0].accessProtocol == fileUriParts[0]
      assert responsePage0.items[0].filePath == fileUriParts[1]
      assert responsePage0.items[0].geometry == postBody.geometry
      assert responsePage0.items[0].fileMetadata == postBody.fileMetadata
      assert responsePage0.items.size == 1
      assert responsePage0.totalResults == 1
      assert responsePage0.searchTerms == expSearchTerms0
      assert responsePage0.code == 200
    }

    and: 'etl can search for the 2nd page'
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

      assert responsePage0.items.size == 1
      assert responsePage1.items.size == 1
      assert responsePage0 != responsePage1
    }
  }
}

