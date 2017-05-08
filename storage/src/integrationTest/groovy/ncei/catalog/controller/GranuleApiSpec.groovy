package ncei.catalog.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import static org.hamcrest.Matchers.equalTo
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class GranuleApiSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def 'create, read, update, delete granule metadata'() {
    setup: 'define a granule metadata record'
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

    when: 'we post, a new record is create and returned in response'
    Map granuleMetadata = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
          .when()
            .post('/granules')
          .then()
            .assertThat()
            .statusCode(200)  //should be a 201
            .body('granule.tracking_id', equalTo(postBody.tracking_id))
            .body('granule.filename', equalTo(postBody.filename))
            .body('granule.granule_size', equalTo(postBody.granule_size))
            .body('granule.granule_schema', equalTo(postBody.granule_schema))
            .body('granule.geometry', equalTo(postBody.geometry))
            .body('granule.access_protocol', equalTo(postBody.access_protocol))
            .body('granule.type', equalTo(postBody.type))
            .body('granule.granule_metadata', equalTo(postBody.granule_metadata))
            .body('granule.collections', equalTo(postBody.collections))
          .extract()
            .path('granule')

    then: 'we can get it by id'
    RestAssured.given()
            .contentType(ContentType.JSON)
          .when()
            .get("/granules/${granuleMetadata.granule_id}")
          .then()
            .assertThat()
            .statusCode(200)  
            .body('granules[0].tracking_id', equalTo(postBody.tracking_id))
            .body('granules[0].filename', equalTo(postBody.filename))
            .body('granules[0].granule_size', equalTo(postBody.granule_size))
            .body('granules[0].granule_schema', equalTo(postBody.granule_schema))
            .body('granules[0].geometry', equalTo(postBody.geometry))
            .body('granules[0].access_protocol', equalTo(postBody.access_protocol))
            .body('granules[0].type', equalTo(postBody.type))
            .body('granules[0].granule_metadata', equalTo(postBody.granule_metadata))
            .body('granules[0].collections', equalTo(postBody.collections))

    when: 'we update the postBody with the granule_id and new metadata'

    String updatedMetadata = "different metadata"
    Map updatedPostBody = granuleMetadata.clone()
    updatedPostBody.granule_metadata = updatedMetadata

    then: 'we can update it (create a new version)'

    RestAssured.given()
            .body(updatedPostBody)
            .contentType(ContentType.JSON)
          .when()
            .put("/granules/${granuleMetadata.granule_id}")
          .then()
            .assertThat()
            .statusCode(200)  //should be a 201
            .body('granule.tracking_id', equalTo(postBody.tracking_id))
            .body('granule.filename', equalTo(postBody.filename))
            .body('granule.granule_size', equalTo(postBody.granule_size))
            .body('granule.granule_schema', equalTo(postBody.granule_schema))
            .body('granule.geometry', equalTo(postBody.geometry))
            .body('granule.access_protocol', equalTo(postBody.access_protocol))
            .body('granule.type', equalTo(postBody.type))
            .body('granule.granule_metadata', equalTo(updatedMetadata))
            .body('granule.collections', equalTo(postBody.collections))


    and: 'we can get both versions'
    Map updatedRecord = RestAssured.given()
            .param('showVersions', true)
          .when()
            .get("/granules/${granuleMetadata.granule_id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('totalResults', equalTo(2))
    //first one is the newest
            .body('granules[0].tracking_id', equalTo(postBody.tracking_id))
            .body('granules[0].filename', equalTo(postBody.filename))
            .body('granules[0].granule_size', equalTo(postBody.granule_size))
            .body('granules[0].granule_schema', equalTo(postBody.granule_schema))
            .body('granules[0].geometry', equalTo(postBody.geometry))
            .body('granules[0].access_protocol', equalTo(postBody.access_protocol))
            .body('granules[0].type', equalTo(postBody.type))
            .body('granules[0].granule_metadata', equalTo(updatedMetadata))
            .body('granules[0].collections', equalTo(postBody.collections))

    //second one is the original
            .body('granules[1].tracking_id', equalTo(postBody.tracking_id))
            .body('granules[1].filename', equalTo(postBody.filename))
            .body('granules[1].granule_size', equalTo(postBody.granule_size))
            .body('granules[1].granule_schema', equalTo(postBody.granule_schema))
            .body('granules[1].geometry', equalTo(postBody.geometry))
            .body('granules[1].access_protocol', equalTo(postBody.access_protocol))
            .body('granules[1].type', equalTo(postBody.type))
            .body('granules[1].granule_metadata', equalTo(postBody.granule_metadata))
            .body('granules[1].collections', equalTo(postBody.collections))
          .extract()
            .path('granules[0]')

    then: 'submit the latest granule back with a delete method to delete it'
    //delete it
    RestAssured.given()
            .body(updatedRecord)
            .contentType(ContentType.JSON)
          .when()
            .delete("/granules/${granuleMetadata.granule_id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('message' as String, equalTo('Successfully deleted row with id: ' + updatedPostBody.granule_id))

    and: 'it is gone, but we can get it with a a flag- showDeleted'
    RestAssured.given()
          .when()
            .get("/granules/${granuleMetadata.granule_id}")
          .then()
            .assertThat()
            .contentType(ContentType.JSON)
            .statusCode(404)  //should be a 404
            .body('granules', equalTo([]))

    RestAssured.given()
            .param('showDeleted', true)
          .when()
            .get("/granules/${granuleMetadata.granule_id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('granules[0].tracking_id', equalTo(postBody.tracking_id))
            .body('granules[0].filename', equalTo(postBody.filename))
            .body('granules[0].granule_size', equalTo(postBody.granule_size))
            .body('granules[0].granule_schema', equalTo(postBody.granule_schema))
            .body('granules[0].geometry', equalTo(postBody.geometry))
            .body('granules[0].access_protocol', equalTo(postBody.access_protocol))
            .body('granules[0].type', equalTo(postBody.type))
            .body('granules[0].granule_metadata', equalTo(updatedMetadata))
            .body('granules[0].collections', equalTo(postBody.collections))
            .body('granules[0].deleted', equalTo(true))

    and: 'we can get all 3 back with showDeleted AND showVersions'
    RestAssured.given()
            .param('showDeleted', true)
            .param('showVersions', true)
          .when()
            .get("/granules/${granuleMetadata.granule_id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('totalResults', equalTo(3))
            .body('granules[0].granule_schema', equalTo(postBody.granule_schema))
            .body('granules[0].granule_size', equalTo(postBody.granule_size))
            .body('granules[0].granule_metadata', equalTo(updatedMetadata))
            .body('granules[0].geometry', equalTo(postBody.geometry))
            .body('granules[0].type', equalTo(postBody.type))
            .body('granules[0].deleted', equalTo(true))

            .body('granules[1].granule_schema', equalTo(postBody.granule_schema))
            .body('granules[1].granule_size', equalTo(postBody.granule_size))
            .body('granules[1].granule_metadata', equalTo(updatedMetadata))
            .body('granules[1].geometry', equalTo(postBody.geometry))
            .body('granules[1].type', equalTo(postBody.type))
            .body('granules[1].deleted', equalTo(false))

            .body('granules[2].granule_schema', equalTo(postBody.granule_schema))
            .body('granules[2].granule_size', equalTo(postBody.granule_size))
            .body('granules[2].granule_metadata', equalTo(postBody.granule_metadata))
            .body('granules[2].geometry', equalTo(postBody.geometry))
            .body('granules[2].type', equalTo(postBody.type))
            .body('granules[2].deleted', equalTo(false))


    then: 'clean up the db, purge all 3 records by id'
    //delete all with that granule_id
    RestAssured.given()
            .body(updatedRecord) //id in here
            .contentType(ContentType.JSON)
          .when()
            .delete('/granules/purge')
          .then()
            .assertThat()
            .statusCode(200)
            .body('message' as String, equalTo('Successfully purged 3 rows matching ' + updatedRecord))
  }
}
