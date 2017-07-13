package org.cedar.metadata.api

import io.restassured.RestAssured
import io.restassured.http.ContentType

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import static org.hamcrest.Matchers.equalTo
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [ApiApplication], webEnvironment = RANDOM_PORT)
class GranulesEndpointSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  static final String STORAGE_GRANULES_ENDPOINT = '/storage/granules'
  static final String INDEX_ENDPOINT = '/index/search'

  def postBody = [
      "filename"       : "granuleFace",
      "type"           : "fos",
      "access_protocol": "FILE",
      "file_path"      : "path",
      "size_bytes"     : 1024,
      "metadata"       : "{blah:blah}",
      "geometry"       : "POLYGON()",
      "collections"    : ["a", "list", "of", "collections"]
  ]

  def poller = new PollingConditions(timeout: 5)

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  @Unroll
  def 'CRUD operations on a granule'() {

    when: 'Create: save a new granule'
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
    String id = granuleMetadata.id

    then: 'id returned is not null'
    id != null

    and: 'eventually searching the index finds the record'
    Map response
    poller.eventually {
      response = RestAssured.given()
          .contentType(ContentType.JSON)
          .params([q: "_id:$id"])
          .when()
          .get(INDEX_ENDPOINT)
          .then()
          .assertThat()
          .statusCode(200)
          .extract().response().as(Map)

      assert response.data.size == 1
      assert response.data[0].id == id
    }

    when: 'Update: we update the granule'
    String updatedFilename = 'a different filename'
    granuleMetadata.filename = updatedFilename
    granuleMetadata = RestAssured.given()
        .body(granuleMetadata)
        .contentType(ContentType.JSON)
        .when()
        .put("$STORAGE_GRANULES_ENDPOINT/$id")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data[0].id', equalTo(id))
        .extract().path('data[0].attributes')
    assert granuleMetadata.filename == updatedFilename

    then: 'we can get the updated record from storage'
    RestAssured.given()
        .body(granuleMetadata)
        .contentType(ContentType.JSON)
        .when()
        .get("$STORAGE_GRANULES_ENDPOINT/$id")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data[0].id', equalTo(id))
        .body('data[0].attributes.filename', equalTo(updatedFilename))

    and: 'index got the update'
    poller.eventually {
      response = RestAssured.given()
          .contentType(ContentType.JSON)
          .params([q: "_id:$id"])
          .when()
          .get(INDEX_ENDPOINT)
          .then()
          .assertThat()
          .statusCode(200)
          .extract().response().as(Map)

      assert response.data.size == 1
      assert response.data[0].id == id
      assert response.data[0].attributes.filename == updatedFilename
    }

    when: 'Delete: we delete the granule'
    RestAssured.given()
        .body(granuleMetadata)
        .contentType(ContentType.JSON)
        .when()
        .delete("$STORAGE_GRANULES_ENDPOINT/$id")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data[0].id', equalTo(id))
        .body('data[0].attributes.deleted', equalTo(true))

    then: 'it is deleted from storage'
    poller.eventually {
      RestAssured.given()
          .body(granuleMetadata)
          .contentType(ContentType.JSON)
          .when()
          .get("$STORAGE_GRANULES_ENDPOINT/$id")
          .then()
          .assertThat()
          .statusCode(404)
    }

    and: 'it is deleted in the index'
    poller.eventually {
      response = RestAssured.given()
          .contentType(ContentType.JSON)
          .params([q: "_id:$granuleMetadata.id"])
          .when()
          .get("index/search")
          .then()
          .assertThat()
          .statusCode(200)
          .extract().response().as(Map)

      assert response.data.size == 0
    }
  }
}
