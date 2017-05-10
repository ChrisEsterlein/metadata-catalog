package ncei.catalog.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Ignore
import spock.lang.Specification

import static org.hamcrest.Matchers.equalTo
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@Ignore
@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class CollectionApiSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def 'create, read, update, delete collection metadata'() {
    setup: 'define a collection metadata record'
    def postBody = [
            "collection_name"    : "collectionFace",
            "collection_schema"  : "a collection schema",
            "type"               : "fos",
            "collection_metadata": "{blah:blah}"
    ]

    when: 'we post, a new record is create and returned in response'
    Map collectionMetadata = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
          .when()
            .post('/collections')
          .then()
            .assertThat()
            .statusCode(200)  //should be a 201
            .body('collection.collection_name', equalTo(postBody.collection_name))
            .body('collection.collection_size', equalTo(postBody.collection_size))
            .body('collection.collection_metadata', equalTo(postBody.collection_metadata))
            .body('collection.geometry', equalTo(postBody.geometry))
            .body('collection.type', equalTo(postBody.type))
          .extract()
            .path('collection')

    then: 'we can get it by id'
    RestAssured.given()
            .contentType(ContentType.JSON)
          .when()
            .get("/collections/${collectionMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)  //should be a 201
            .body('collections[0].collection_name', equalTo(postBody.collection_name))
            .body('collections[0].collection_size', equalTo(postBody.collection_size))
            .body('collections[0].collection_metadata', equalTo(postBody.collection_metadata))
            .body('collections[0].geometry', equalTo(postBody.geometry))
            .body('collections[0].type', equalTo(postBody.type))


    when: 'we update the postBody with the id and new metadata'

    String updatedMetadata = "different metadata"
    Map updatedPostBody = collectionMetadata.clone()
    updatedPostBody.collection_metadata = updatedMetadata

    then: 'we can update it (create a new version)'

    RestAssured.given()
            .body(updatedPostBody)
            .contentType(ContentType.JSON)
          .when()
            .put("/collections/${collectionMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)  //should be a 201
            .body('collection.collection_name', equalTo(postBody.collection_name))
            .body('collection.collection_size', equalTo(postBody.collection_size))
            .body('collection.collection_metadata', equalTo(updatedMetadata))
            .body('collection.geometry', equalTo(postBody.geometry))
            .body('collection.type', equalTo(postBody.type))

    and: 'we can get both versions'
    Map updatedRecord = RestAssured.given()
            .param('showVersions', true)
          .when()
            .get("/collections/${collectionMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('totalResults', equalTo(2))
    //first one is the newest
            .body('collections[0].collection_name', equalTo(postBody.collection_name))
            .body('collections[0].collection_size', equalTo(postBody.collection_size))
            .body('collections[0].collection_metadata', equalTo(updatedMetadata))
            .body('collections[0].geometry', equalTo(postBody.geometry))
            .body('collections[0].type', equalTo(postBody.type))
    //second one is the original
            .body('collections[1].collection_name', equalTo(postBody.collection_name))
            .body('collections[1].collection_size', equalTo(postBody.collection_size))
            .body('collections[1].collection_metadata', equalTo(postBody.collection_metadata))
            .body('collections[1].geometry', equalTo(postBody.geometry))
            .body('collections[1].type', equalTo(postBody.type))
          .extract()
            .path('collections[0]')

    then: 'submit the latest collection back with a delete method to delete it'
    //delete it
    RestAssured.given()
            .body(updatedRecord)
            .contentType(ContentType.JSON)
          .when()
            .delete("/collections/${collectionMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('message' as String, equalTo('Successfully deleted row with id: ' + updatedPostBody.id))

    and: 'it is gone, but we can get it with a a flag- showDeleted'
    RestAssured.given()
          .when()
            .get("/collections/${collectionMetadata.id}")
          .then()
            .assertThat()
            .contentType(ContentType.JSON)
            .statusCode(404)  //should be a 404
            .body('collections', equalTo([]))

    RestAssured.given()
            .param('showDeleted', true)
          .when()
            .get("/collections/${collectionMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('totalResults', equalTo(1))
            .body('collections[0].collection_name', equalTo(postBody.collection_name))
            .body('collections[0].collection_size', equalTo(postBody.collection_size))
            .body('collections[0].collection_metadata', equalTo(updatedMetadata))
            .body('collections[0].geometry', equalTo(postBody.geometry))
            .body('collections[0].type', equalTo(postBody.type))
            .body('collections[0].deleted', equalTo(true))

    and: 'we can get all 3 back with showDeleted AND showVersions'
    RestAssured.given()
            .param('showDeleted', true)
            .param('showVersions', true)
          .when()
            .get("/collections/${collectionMetadata.id}")
          .then()
            .assertThat()
            .statusCode(200)
            .body('totalResults', equalTo(3))
            .body('collections[0].collection_name', equalTo(postBody.collection_name))
            .body('collections[0].collection_size', equalTo(postBody.collection_size))
            .body('collections[0].collection_metadata', equalTo(updatedMetadata))
            .body('collections[0].geometry', equalTo(postBody.geometry))
            .body('collections[0].type', equalTo(postBody.type))
            .body('collections[0].deleted', equalTo(true))
            .body('collections[1].collection_name', equalTo(postBody.collection_name))
            .body('collections[1].collection_size', equalTo(postBody.collection_size))
            .body('collections[1].collection_metadata', equalTo(updatedMetadata))
            .body('collections[1].geometry', equalTo(postBody.geometry))
            .body('collections[1].type', equalTo(postBody.type))
            .body('collections[2].collection_name', equalTo(postBody.collection_name))
            .body('collections[2].collection_size', equalTo(postBody.collection_size))
            .body('collections[2].collection_metadata', equalTo(postBody.collection_metadata))
            .body('collections[2].geometry', equalTo(postBody.geometry))
            .body('collections[2].type', equalTo(postBody.type))


    then: 'clean up the db, purge all 3 records by id'
    //delete all with that id
    RestAssured.given()
            .body(updatedRecord) //id in here
            .contentType(ContentType.JSON)
          .when()
            .delete('/collections/purge')
          .then()
            .assertThat()
            .statusCode(200)
            .body('message' as String, equalTo('Successfully purged 3 rows matching ' + updatedRecord))
  }
}
