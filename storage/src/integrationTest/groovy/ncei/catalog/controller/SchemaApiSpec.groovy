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
class SchemaApiSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def 'create, read, update, delete schema metadata'() {
    setup: 'define a schema metadata record'
    def postBody = [
            "schema_name"  : "schemaFace",
            "schema_schema": "a schema schema",
            "json_schema"  : "{blah:blah}"
    ]

    when: 'we post, a new record is create and returned in response'
    Map schemaMetadata = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
          .when()
            .post('/schemas')
          .then()
            .assertThat()
            .statusCode(200)  //should be a 201
            .body('schema.schema_name', equalTo(postBody.schema_name))
            .body('schema.schema_size', equalTo(postBody.schema_size))
            .body('schema.json_schema', equalTo(postBody.json_schema))
            .body('schema.geometry', equalTo(postBody.geometry))
            .extract()
            .path('schema')

    then: 'we can get it by id'
    RestAssured.given()
            .contentType(ContentType.JSON)
          .when()
            .get("/schemas/${schemaMetadata.schema_id}")
          .then()
            .assertThat()
            .statusCode(200)  //should be a 201
            .body('schemas[0].schema_name', equalTo(postBody.schema_name))
            .body('schemas[0].schema_size', equalTo(postBody.schema_size))
            .body('schemas[0].json_schema', equalTo(postBody.json_schema))
            .body('schemas[0].geometry', equalTo(postBody.geometry))


    when: 'we update the postBody with the schema_id and new metadata'

    String updatedMetadata = "different metadata"
    Map updatedPostBody = schemaMetadata.clone()
    updatedPostBody.json_schema = updatedMetadata

    then: 'we can update it (create a new version)'

    RestAssured.given()
            .body(updatedPostBody)
            .contentType(ContentType.JSON)
          .when()
            .put("/schemas/${schemaMetadata.schema_id}")
          .then()
            .assertThat()
            .statusCode(200)  //should be a 201
            .body('schema.schema_name', equalTo(postBody.schema_name))
            .body('schema.schema_size', equalTo(postBody.schema_size))
            .body('schema.json_schema', equalTo(updatedMetadata))
            .body('schema.geometry', equalTo(postBody.geometry))

    and: 'we can get both versions'
    Map updatedRecord = RestAssured.given()
            .param('showVersions', true)
          .when()
            .get("/schemas/${schemaMetadata.schema_id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('totalResults', equalTo(2))
    //first one is the newest
            .body('schemas[0].schema_name', equalTo(postBody.schema_name))
            .body('schemas[0].schema_size', equalTo(postBody.schema_size))
            .body('schemas[0].json_schema', equalTo(updatedMetadata))
            .body('schemas[0].geometry', equalTo(postBody.geometry))
    //second one is the original
            .body('schemas[1].schema_name', equalTo(postBody.schema_name))
            .body('schemas[1].schema_size', equalTo(postBody.schema_size))
            .body('schemas[1].json_schema', equalTo(postBody.json_schema))
            .body('schemas[1].geometry', equalTo(postBody.geometry))
          .extract()
            .path('schemas[0]')

    then: 'submit the latest schema back with a delete method to delete it'
    //delete it
    RestAssured.given()
            .body(updatedRecord)
            .contentType(ContentType.JSON)
          .when()
            .delete("/schemas/${schemaMetadata.schema_id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('message' as String, equalTo('Successfully deleted row with schema_id: ' + updatedPostBody.schema_id))

    and: 'it is gone, but we can get it with a a flag- showDeleted'
    RestAssured.given()
          .when()
            .get("/schemas/${schemaMetadata.schema_id}")
          .then()
            .assertThat()
            .contentType(ContentType.JSON)
            .statusCode(404)  //should be a 404
            .body('schemas', equalTo([]))

    RestAssured.given()
            .param('showDeleted', true)
          .when()
            .get("/schemas/${schemaMetadata.schema_id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('totalResults', equalTo(1))
            .body('schemas[0].schema_name', equalTo(postBody.schema_name))
            .body('schemas[0].schema_size', equalTo(postBody.schema_size))
            .body('schemas[0].json_schema', equalTo(updatedMetadata))
            .body('schemas[0].geometry', equalTo(postBody.geometry))
            .body('schemas[0].deleted', equalTo(true))

    and: 'we can get all 3 back with showDeleted AND showVersions'
    RestAssured.given()
            .param('showDeleted', true)
            .param('showVersions', true)
          .when()
            .get("/schemas/${schemaMetadata.schema_id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('totalResults', equalTo(3))
            .body('schemas[0].schema_name', equalTo(postBody.schema_name))
            .body('schemas[0].schema_size', equalTo(postBody.schema_size))
            .body('schemas[0].json_schema', equalTo(updatedMetadata))
            .body('schemas[0].geometry', equalTo(postBody.geometry))
            .body('schemas[0].deleted', equalTo(true))
            .body('schemas[1].schema_name', equalTo(postBody.schema_name))
            .body('schemas[1].schema_size', equalTo(postBody.schema_size))
            .body('schemas[1].json_schema', equalTo(updatedMetadata))
            .body('schemas[1].geometry', equalTo(postBody.geometry))
            .body('schemas[2].schema_name', equalTo(postBody.schema_name))
            .body('schemas[2].schema_size', equalTo(postBody.schema_size))
            .body('schemas[2].json_schema', equalTo(postBody.json_schema))
            .body('schemas[2].geometry', equalTo(postBody.geometry))

    then: 'clean up the db, purge all 3 records by id'
    //delete all with that schema_id
    RestAssured.given()
            .body(updatedRecord) //id in here
            .contentType(ContentType.JSON)
          .when()
            .delete('/schemas/purge')
          .then()
            .assertThat()
            .statusCode(200)
            .body('message' as String, equalTo('Successfully purged 3 rows matching ' + updatedRecord))
  }
}