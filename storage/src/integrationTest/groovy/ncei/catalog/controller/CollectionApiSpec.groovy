package ncei.catalog.controller

import groovy.json.JsonSlurper
import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.hamcrest.Matchers.equalTo
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class CollectionApiSpec extends Specification {

  @Autowired
  RabbitTemplate rabbitTemplate

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  PollingConditions poller

  def setup() {
    poller = new PollingConditions(timeout: 10)
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
            "collection_metadata": "{blah:blah}",
            "geometry"           : "point()"
    ]

    when: 'we post, a new record is create and returned in response'
    Map collectionMetadata = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
            .when()
            .post('/collections')
            .then()
            .assertThat()
            .statusCode(201)  //should be a 201
            .body('data[0].attributes.collection_name', equalTo(postBody.collection_name))
            .body('data[0].attributes.collection_schema', equalTo(postBody.collection_schema))
            .body('data[0].attributes.collection_metadata', equalTo(postBody.collection_metadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .extract()
            .path('data[0].attributes')

    then: 'we can get it by id'
    RestAssured.given()
            .contentType(ContentType.JSON)
            .when()
            .get("/collections/${collectionMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('data[0].attributes.collection_name', equalTo(postBody.collection_name))
            .body('data[0].attributes.collection_schema', equalTo(postBody.collection_schema))
            .body('data[0].attributes.collection_metadata', equalTo(postBody.collection_metadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .extract().body()


    when: 'we update the postBody with the id and new metadata'

    String updatedMetadata = "different metadata"
    Map updatedPostBody = collectionMetadata.clone() as Map
    updatedPostBody.collection_metadata = updatedMetadata

    then: 'we can update the record (create a new version)'

    RestAssured.given()
            .body(updatedPostBody)
            .contentType(ContentType.JSON)
            .when()
            .put("/collections/${collectionMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('data[0].attributes.id', equalTo(collectionMetadata.id))
            .body('data[0].attributes.collection_name', equalTo(postBody.collection_name))
            .body('data[0].attributes.collection_schema', equalTo(postBody.collection_schema))
            .body('data[0].attributes.collection_metadata', equalTo(updatedPostBody.collection_metadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))

    and: 'we can get both versions'
    Map updatedRecord = RestAssured.given()
            .param('showVersions', true)
            .when()
            .get("/collections/${collectionMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('meta.totalResults', equalTo(2))
    //first one is the newest
            .body('data[0].attributes.collection_name', equalTo(postBody.collection_name))
            .body('data[0].attributes.collection_schema', equalTo(postBody.collection_schema))
            .body('data[0].attributes.collection_metadata', equalTo(updatedMetadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))
    //second one is the original
            .body('data[1].attributes.collection_name', equalTo(postBody.collection_name))
            .body('data[1].attributes.collection_schema', equalTo(postBody.collection_schema))
            .body('data[1].attributes.collection_metadata', equalTo(postBody.collection_metadata))
            .body('data[1].attributes.geometry', equalTo(postBody.geometry))
            .body('data[1].attributes.type', equalTo(postBody.type))
            .extract()
            .path('data[0].attributes')

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
            .body('meta.message' as String, equalTo('Successfully deleted row with id: ' + updatedPostBody.id))

    and: 'it is gone, but we can get it with a a flag- showDeleted'
    RestAssured.given()
            .param('showVersions', true)
            .when()
            .get("/collections/${collectionMetadata.id}")
            .then()
            .assertThat()
            .contentType(ContentType.JSON)
            .statusCode(404)
            .body('data', equalTo(null))
            .body('errors', equalTo(['No results found.']))

    RestAssured.given()
            .param('showDeleted', true)
            .when()
            .get("/collections/${collectionMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('meta.totalResults', equalTo(1))
            .body('data[0].attributes.collection_name', equalTo(postBody.collection_name))
            .body('data[0].attributes.collection_schema', equalTo(postBody.collection_schema))
            .body('data[0].attributes.collection_metadata', equalTo(updatedMetadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .body('data[0].attributes.deleted', equalTo(true))

    and: 'we can get all 3 back with showDeleted AND showVersions'
    RestAssured.given()
            .param('showDeleted', true)
            .param('showVersions', true)
            .when()
            .get("/collections/${collectionMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('meta.code', equalTo(200))
            .body('meta.success', equalTo(true))
            .body('meta.action', equalTo('read'))
            .body('meta.totalResults', equalTo(3))

            .body('data[0].attributes.collection_name', equalTo(postBody.collection_name))
            .body('data[0].attributes.collection_schema', equalTo(postBody.collection_schema))
            .body('data[0].attributes.collection_metadata', equalTo(updatedMetadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .body('data[0].attributes.deleted', equalTo(true))

            .body('data[1].attributes.collection_name', equalTo(postBody.collection_name))
            .body('data[1].attributes.collection_schema', equalTo(postBody.collection_schema))
            .body('data[1].attributes.collection_metadata', equalTo(updatedMetadata))
            .body('data[1].attributes.geometry', equalTo(postBody.geometry))
            .body('data[1].attributes.type', equalTo(postBody.type))

            .body('data[2].attributes.collection_name', equalTo(postBody.collection_name))
            .body('data[2].attributes.collection_schema', equalTo(postBody.collection_schema))
            .body('data[2].attributes.collection_metadata', equalTo(postBody.collection_metadata))
            .body('data[2].attributes.geometry', equalTo(postBody.geometry))
            .body('data[2].attributes.type', equalTo(postBody.type))


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
            .body('meta.id', equalTo(updatedRecord.id))
            .body('meta.totalResultsDeleted', equalTo(3))
            .body('meta.success', equalTo(true))

    and: 'finally, we should have sent 3 messages'

    List<String> actions = []

    poller.eventually {
      String m
      List<String> expectedActions = ['insert', 'update', 'delete']
      while (m = (rabbitTemplate.receive('index-consumer'))?.getBodyContentAsString()) {
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(m)
        actions.add(object.meta.action)
        assert actions == expectedActions
      }
    }
  }
}
