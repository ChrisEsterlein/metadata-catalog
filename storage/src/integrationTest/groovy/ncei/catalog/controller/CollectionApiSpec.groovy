package ncei.catalog.controller

import com.datastax.driver.core.utils.UUIDs
import groovy.json.JsonSlurper
import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import ncei.catalog.config.TestRabbitConfig
import ncei.catalog.domain.CollectionMetadata
import ncei.catalog.domain.CollectionMetadataRepository
import ncei.catalog.domain.MetadataRecord
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.isA
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@ActiveProfiles("test")
@SpringBootTest(classes = [Application, TestRabbitConfig], webEnvironment = RANDOM_PORT)
class CollectionApiSpec extends Specification {

  @Autowired
  CollectionMetadataRepository collectionMetadataRepository

  @Autowired
  RabbitTemplate rabbitTemplate

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  @Value('${rabbitmq.queue}')
  String queueName

  PollingConditions poller

  def setup() {
    poller = new PollingConditions(timeout: 10)
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  def postBody = [
      "name"    : "collectionFace",
      "metadata_schema"  : "a collection schema",
      "type"               : "fos",
      "metadata": "{blah:blah}",
      "geometry" : "point()"
  ]

  def 'create and read'() {
    setup: 'define a collection metadata record'

    when: 'we post, a new record is create and returned in response'
    Map collectionMetadata = RestAssured.given()
        .body(postBody)
        .contentType(ContentType.JSON)
        .when()
        .post('/collections')
        .then()
        .assertThat()
        .statusCode(201)  //should be a 201
        .body('data[0].attributes.name', equalTo(postBody.name))
        .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
        .body('data[0].attributes.metadata', equalTo(postBody.metadata))
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
        .body('data[0].attributes.name', equalTo(postBody.name))
        .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
        .body('data[0].attributes.metadata', equalTo(postBody.metadata))
        .body('data[0].attributes.geometry', equalTo(postBody.geometry))
        .body('data[0].attributes.type', equalTo(postBody.type))

    then: 'finally, we should have sent a rabbit message'

    List<String> actions = []

    poller.eventually {
      String m
      List<String> expectedActions = ['insert']
      while (m = (rabbitTemplate.receive(queueName))?.getBodyContentAsString()) {
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(m)
        actions.add(object.data[0].meta.action)
        assert actions == expectedActions
      }
    }

  }

  def 'update and read'() {
    setup: 'define a collection metadata record'
    CollectionMetadata collectionMetadata = collectionMetadataRepository.save(new CollectionMetadata(postBody))

    when: 'we update the postBody with the id and new metadata'

    String updatedMetadata = "different metadata"
    Map updatedPostBody = collectionMetadata.asMap().clone() as Map
    updatedPostBody.metadata = updatedMetadata

    then: 'we can update the record (create a new version)'

    RestAssured.given()
        .body(updatedPostBody)
        .param('version', collectionMetadata.last_update.time)
        .contentType(ContentType.JSON)
        .when()
        .put("/collections/${collectionMetadata.id}")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data[0].id', equalTo(collectionMetadata.id as String))
        .body('data[0].type', equalTo('collection'))

        .body('data[0].attributes.id', equalTo(collectionMetadata.id as String))
        .body('data[0].attributes.name', equalTo(postBody.name))
        .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
        .body('data[0].attributes.metadata', equalTo(updatedPostBody.metadata))
        .body('data[0].attributes.geometry', equalTo(postBody.geometry))
        .body('data[0].attributes.type', equalTo(postBody.type))

    then: 'by default we only get the latest version'
    RestAssured.given()
        .when()
        .get("/collections/${collectionMetadata.id}")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data[0].attributes.name', equalTo(postBody.name))
        .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
        .body('data[0].attributes.metadata', equalTo(updatedMetadata))
        .body('data[0].attributes.geometry', equalTo(postBody.geometry))
        .body('data[0].attributes.type', equalTo(postBody.type))

    and: 'we can get both versions'
    RestAssured.given()
        .param('showVersions', true)
        .when()
        .get("/collections/${collectionMetadata.id}")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data.size', equalTo(2))

    //first one is the newest
        .body('data[0].attributes.name', equalTo(postBody.name))
        .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
        .body('data[0].attributes.metadata', equalTo(updatedMetadata))
        .body('data[0].attributes.geometry', equalTo(postBody.geometry))
        .body('data[0].attributes.type', equalTo(postBody.type))
    //second one is the original
        .body('data[1].attributes.name', equalTo(postBody.name))
        .body('data[1].attributes.metadata_schema', equalTo(postBody.metadata_schema))
        .body('data[1].attributes.metadata', equalTo(postBody.metadata))
        .body('data[1].attributes.geometry', equalTo(postBody.geometry))
        .body('data[1].attributes.type', equalTo(postBody.type))

    then: 'finally, we should have sent a rabbit message'

    List<String> actions = []

    poller.eventually {
      String m
      List<String> expectedActions = ['update']
      while (m = (rabbitTemplate.receive(queueName))?.getBodyContentAsString()) {
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(m)
        actions.add(object.data[0].meta.action)
        assert actions == expectedActions
      }
    }
  }

  def 'update with locking'() {
    when: 'define a collection metadata record'

    Map record = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
            .when()
            .post("/collections")
            .then()
            .assertThat()
            .statusCode(201)
            .body('data[0].type', equalTo('collection'))
            .body('data[0].attributes.name', equalTo(postBody.name))
            .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
            .body('data[0].attributes.metadata', equalTo(postBody.metadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .extract()
            .path('data[0].attributes')

    Long wrongDate =  record.last_update - 1000
    Long correctDate = record.last_update

    then: 'submit the it back with last_update as request param'

    RestAssured.given()
            .param('version', wrongDate)
            .body(record)
            .contentType(ContentType.JSON)
            .when()
            .put("/collections/${record.id}")
            .then()
            .assertThat()
            .statusCode(409)
            .body('errors', isA(List))


    RestAssured.given()
            .param('version', correctDate)
            .body(postBody)
            .contentType(ContentType.JSON)
            .when()
            .put("/collections/${record.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('data[0].id', equalTo(record.id as String))
            .body('data[0].attributes.id', equalTo(record.id as String))
            .body('data[0].attributes.name', equalTo(postBody.name))
            .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
            .body('data[0].attributes.metadata', equalTo(postBody.metadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))
  }

  def 'update without locking'() {
    when: 'define a collection metadata record'

    Map record = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
            .when()
            .post("/collections")
            .then()
            .assertThat()
            .statusCode(201)
            .body('data[0].type', equalTo('collection'))
            .body('data[0].attributes.name', equalTo(postBody.name))
            .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
            .body('data[0].attributes.metadata', equalTo(postBody.metadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))
            .extract()
            .path('data[0].attributes')

    Map updatedRecord = record.clone()
    updatedRecord.metadata = "New metadata"

    then: 'submit it back without version request param '

    RestAssured.given()
            .body(updatedRecord)
            .contentType(ContentType.JSON)
            .when()
            .put("/collections/${record.id}")
            .then()
            .assertThat()
            .statusCode(200)
//            .body('data[0].id', equalTo(record.id as String))
            .body('data[0].type', equalTo('collection'))
            .body('data[0].attributes.id', equalTo(record.id as String))
            .body('data[0].attributes.name', equalTo(postBody.name))
            .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
            .body('data[0].attributes.metadata', equalTo(updatedRecord.metadata))
            .body('data[0].attributes.geometry', equalTo(postBody.geometry))
            .body('data[0].attributes.type', equalTo(postBody.type))

  }

    def 'delete and read'() {
    setup: 'define a collection metadata record'
    CollectionMetadata collectionMetadata = collectionMetadataRepository.save(new CollectionMetadata(postBody))

    when: 'submit the latest collection back with a delete method to delete it'

    Map updatedPostBody = collectionMetadata.asMap().clone() as Map

    //delete it
    RestAssured.given()
        .body(updatedPostBody)
        .contentType(ContentType.JSON)
        .when()
        .delete("/collections/${collectionMetadata.id}")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data[0].meta.action', equalTo('delete'))
        .body('data[0].id', equalTo(collectionMetadata.id as String))

    then: 'it is gone, but we can get it with a a flag- showDeleted'
    RestAssured.given()
        .param('showVersions', true)
        .when()
        .get("/collections/${collectionMetadata.id}")
        .then()
        .assertThat()
        .contentType(ContentType.JSON)
        .statusCode(404)
        .body('data', equalTo(null))
        .body('errors', equalTo(['No records exist with id: ' + collectionMetadata.id.toString()]))

    RestAssured.given()
        .param('showDeleted', true)
        .when()
        .get("/collections/${collectionMetadata.id}")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data.size', equalTo(1))
        .body('data[0].attributes.name', equalTo(postBody.name))
        .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
        .body('data[0].attributes.metadata', equalTo(postBody.metadata))
        .body('data[0].attributes.geometry', equalTo(postBody.geometry))
        .body('data[0].attributes.type', equalTo(postBody.type))
        .body('data[0].attributes.deleted', equalTo(true))

    and: 'we can get everything back with showDeleted AND showVersions'
    RestAssured.given()
        .param('showDeleted', true)
        .param('showVersions', true)
        .when()
        .get("/collections/${collectionMetadata.id}")
        .then()
        .assertThat()
        .statusCode(200)
        .body('data.size', equalTo(2))
        .body('data[0].attributes.name', equalTo(postBody.name))
        .body('data[0].attributes.metadata_schema', equalTo(postBody.metadata_schema))
        .body('data[0].attributes.metadata', equalTo(postBody.metadata))
        .body('data[0].attributes.geometry', equalTo(postBody.geometry))
        .body('data[0].attributes.type', equalTo(postBody.type))
        .body('data[0].attributes.deleted', equalTo(true))

        .body('data[1].attributes.name', equalTo(postBody.name))
        .body('data[1].attributes.metadata_schema', equalTo(postBody.metadata_schema))
        .body('data[1].attributes.metadata', equalTo(postBody.metadata))
        .body('data[1].attributes.geometry', equalTo(postBody.geometry))
        .body('data[1].attributes.type', equalTo(postBody.type))

    then: 'finally, we should have sent a rabbit message'

    List<String> actions = []

    poller.eventually {
      String m
      List<String> expectedActions = ['delete']
      while (m = (rabbitTemplate.receive(queueName))?.getBodyContentAsString()) {
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(m)
        actions.add(object.data[0].meta.action)
        assert actions == expectedActions
      }
    }

  }

  def 'trigger recovery - only latest version is sent'(){
    setup:

    collectionMetadataRepository.deleteAll()

    when: 'we trigger the recovery process'

    MetadataRecord record = collectionMetadataRepository.save(new CollectionMetadata(postBody))

    //create two records with same id
    UUID sharedId = UUIDs.timeBased()
    Map updatedPostBody = postBody.clone()
    updatedPostBody.id = sharedId
    MetadataRecord oldVersion = collectionMetadataRepository.save(new CollectionMetadata(updatedPostBody))
    MetadataRecord latestVersion = collectionMetadataRepository.save(new CollectionMetadata(updatedPostBody))

    RestAssured.given()
            .contentType(ContentType.JSON)
            .when()
            .put('/collections/recover')
            .then()
            .assertThat()
            .statusCode(200)

    then:
    poller.eventually {
      String m
      while (m = (rabbitTemplate.receive(queueName))?.getBodyContentAsString()) {
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(m)
        assert (object.data[0] == record || object.data[0] == latestVersion) && !(object.data[0] == oldVersion)
      }
    }

  }

  def 'messages are sent with appropriate action'(){
    setup:

    collectionMetadataRepository.deleteAll()

    MetadataRecord original = collectionMetadataRepository.save(new CollectionMetadata(postBody))

    Map updatedPostBody = postBody.clone()
    updatedPostBody.deleted = true
    CollectionMetadata deletedVersion = new CollectionMetadata(updatedPostBody)

    MetadataRecord deleted = collectionMetadataRepository.save(deletedVersion)

    when: 'we trigger the recovery process'
    RestAssured.given()
            .contentType(ContentType.JSON)
            .when()
            .put('/collections/recover')
            .then()
            .assertThat()
            .statusCode(200)

    then:

    poller.eventually {
      String m
      while (m = (rabbitTemplate.receive(queueName))?.getBodyContentAsString()) {
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(m)
        if(object.data[0].meta.action == 'update'){
          assert object.data[0].id == original.id
        }
        if(object.data[0].meta.action == 'delete'){
          assert object.data[0].id == deleted.id
        }
      }
    }
  }

  def 'controller advice catches 404'() {

    expect:
    String badPath = '/noSuchEndpoint'

    RestAssured.given()
            .contentType(ContentType.JSON)
            .when()
            .get(badPath)
            .then()
            .assertThat()
            .statusCode(404)
            .body('meta.message', equalTo('Not Found'))
            .body('errors', isA(List))
            .body('errors[0]', containsString("No handler found for GET $badPath"))
  }
}
