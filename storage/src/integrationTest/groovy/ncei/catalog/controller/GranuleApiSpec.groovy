package ncei.catalog.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Ignore
import spock.lang.Specification

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.not
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

    when: 'we post, a new record is created and returned in response'
    Map granuleMetadata = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
          .when()
            .post('/granules')
          .then()
            .assertThat()
            .statusCode(201)
            .body('data[0].type', equalTo('granule'))
            .body('data[0].attributes.tracking_id', equalTo(postBody.tracking_id))
            .body('data[0].attributes.filename', equalTo(postBody.filename))
            .body('data[0].attributes.granule_size', equalTo(postBody.granule_size))
            .body('data[0].attributes.granule_schema', equalTo(postBody.granule_schema))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.access_protocol', equalTo(postBody.access_protocol))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .body('data[0].attributes.granule_metadata', equalTo(postBody.granule_metadata))
            .body('data[0].attributes.collections', equalTo(postBody.collections))
          .extract()
            .path('data[0].attributes')

    then: 'we can get it by id'
    RestAssured.given()
            .contentType(ContentType.JSON)
          .when()
            .get("/granules/${granuleMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('data[0].type', equalTo('granule'))
            .body('data[0].id', equalTo(granuleMetadata.id))
            .body('data[0].attributes.tracking_id', equalTo(postBody.tracking_id))
            .body('data[0].attributes.filename', equalTo(postBody.filename))
            .body('data[0].attributes.granule_size', equalTo(postBody.granule_size))
            .body('data[0].attributes.granule_schema', equalTo(postBody.granule_schema))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.access_protocol', equalTo(postBody.access_protocol))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .body('data[0].attributes.granule_metadata', equalTo(postBody.granule_metadata))
            .body('data[0].attributes.collections', equalTo(postBody.collections))

    when: 'we update the postBody with the id and new metadata'

    String updatedMetadata = "different metadata"
    Map updatedPostBody = granuleMetadata.clone()
    updatedPostBody.granule_metadata = updatedMetadata

    then: 'we can update the record (create a new version)'

    RestAssured.given()
            .body(updatedPostBody)
            .contentType(ContentType.JSON)
          .when()
            .put("/granules/${granuleMetadata.id}")
          .then()
            .assertThat()
//            .statusCode(200)
            .body('data[0].type', equalTo('granule'))
            .body('data[0].id', equalTo(granuleMetadata.id))
            .body('data[0].attributes.last_update', not(granuleMetadata.last_update))
            .body('data[0].attributes.tracking_id', equalTo(postBody.tracking_id))
            .body('data[0].attributes.filename', equalTo(postBody.filename))
            .body('data[0].attributes.granule_size', equalTo(postBody.granule_size))
            .body('data[0].attributes.granule_schema', equalTo(postBody.granule_schema))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.access_protocol', equalTo(postBody.access_protocol))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .body('data[0].attributes.granule_metadata', equalTo(updatedMetadata))
            .body('data[0].attributes.collections', equalTo(postBody.collections))

    and: 'we can get both versions'
    Map updatedRecord = RestAssured.given()
            .param('showVersions', true)
          .when()
            .get("/granules/${granuleMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('meta.totalResults', equalTo(2))
            .body('meta.code', equalTo(200))
            .body('meta.success', equalTo(true))
            .body('meta.action', equalTo('read'))

    //first one is the newest
            .body('data[0].type', equalTo('granule'))
            .body('data[0].attributes.last_update', not(granuleMetadata.last_update))
            .body('data[0].attributes.tracking_id', equalTo(postBody.tracking_id))
            .body('data[0].attributes.filename', equalTo(postBody.filename))
            .body('data[0].attributes.granule_size', equalTo(postBody.granule_size))
            .body('data[0].attributes.granule_schema', equalTo(postBody.granule_schema))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.access_protocol', equalTo(postBody.access_protocol))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .body('data[0].attributes.granule_metadata', equalTo(updatedMetadata))
            .body('data[0].attributes.collections', equalTo(postBody.collections))

    //second one is the original
            .body('data[1].type', equalTo('granule'))
            .body('data[1].attributes.last_update', equalTo(granuleMetadata.last_update))
            .body('data[1].attributes.tracking_id', equalTo(postBody.tracking_id))
            .body('data[1].attributes.filename', equalTo(postBody.filename))
            .body('data[1].attributes.granule_size', equalTo(postBody.granule_size))
            .body('data[1].attributes.granule_schema', equalTo(postBody.granule_schema))
            .body('data[1].attributes.geometry', equalTo(postBody.geometry))
            .body('data[1].attributes.access_protocol', equalTo(postBody.access_protocol))
            .body('data[1].attributes.type', equalTo(postBody.type))
            .body('data[1].attributes.granule_metadata', equalTo(postBody.granule_metadata))
            .body('data[1].attributes.collections', equalTo(postBody.collections))
          .extract()
            .path('data[0].attributes')

    then: ' we can delete the granule by submitting it back with a delete method'
    //delete it
    RestAssured.given()
            .body(updatedRecord)
            .contentType(ContentType.JSON)
          .when()
            .delete("/granules/${granuleMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('meta.message' as String, equalTo('Successfully deleted row with id: ' + updatedPostBody.id))

    and: 'it is gone, but we can get it with a a flag- showDeleted'
    RestAssured.given()
          .when()
            .get("/granules/${granuleMetadata.id}")
          .then()
            .assertThat()
            .contentType(ContentType.JSON)
            .statusCode(404)  //should be a 404
            .body('data', equalTo(null))
            .body('errors[0]', equalTo('No results found.'))

    RestAssured.given()
            .param('showDeleted', true)
          .when()
            .get("/granules/${granuleMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('data[0].type', equalTo('granule'))
            .body('data[0].attributes.tracking_id', equalTo(postBody.tracking_id))
            .body('data[0].attributes.filename', equalTo(postBody.filename))
            .body('data[0].attributes.granule_size', equalTo(postBody.granule_size))
            .body('data[0].attributes.granule_schema', equalTo(postBody.granule_schema))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.access_protocol', equalTo(postBody.access_protocol))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .body('data[0].attributes.granule_metadata', equalTo(updatedMetadata))
            .body('data[0].attributes.collections', equalTo(postBody.collections))
            .body('data[0].attributes.deleted', equalTo(true))

    and: 'we can get all 3 back with showDeleted AND showVersions'
    RestAssured.given()
            .param('showDeleted', true)
            .param('showVersions', true)
          .when()
            .get("/granules/${granuleMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('meta.totalResults', equalTo(3))
            .body('data[0].type', equalTo('granule'))
            .body('data[0].attributes.granule_schema', equalTo(postBody.granule_schema))
            .body('data[0].attributes.granule_size', equalTo(postBody.granule_size))
            .body('data[0].attributes.granule_metadata', equalTo(updatedMetadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .body('data[0].attributes.deleted', equalTo(true))

            .body('data[1].type', equalTo('granule'))
            .body('data[1].attributes.granule_schema', equalTo(postBody.granule_schema))
            .body('data[1].attributes.granule_size', equalTo(postBody.granule_size))
            .body('data[1].attributes.granule_metadata', equalTo(updatedMetadata))
            .body('data[1].attributes.geometry', equalTo(postBody.geometry))
            .body('data[1].attributes.type', equalTo(postBody.type))
            .body('data[1].attributes.deleted', equalTo(false))

            .body('data[2].type', equalTo('granule'))
            .body('data[2].attributes.granule_schema', equalTo(postBody.granule_schema))
            .body('data[2].attributes.granule_size', equalTo(postBody.granule_size))
            .body('data[2].attributes.granule_metadata', equalTo(postBody.granule_metadata))
            .body('data[2].attributes.geometry', equalTo(postBody.geometry))
            .body('data[2].attributes.type', equalTo(postBody.type))
            .body('data[2].attributes.deleted', equalTo(false))

    then: 'clean up the db, purge all 3 records by id'
    //delete all with that id
    RestAssured.given()
            .body(updatedRecord) //id in here
            .contentType(ContentType.JSON)
          .when()
            .delete('/granules/purge')
          .then()
            .assertThat()
            .statusCode(200)
            .body('meta.id', equalTo(updatedRecord.id))
            .body('meta.totalResultsDeleted', equalTo(3))
            .body('meta.success', equalTo(true))
  }
}
