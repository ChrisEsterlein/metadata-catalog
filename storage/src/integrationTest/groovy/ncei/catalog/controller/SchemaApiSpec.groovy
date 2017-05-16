package ncei.catalog.controller

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session
import groovy.json.JsonSlurper
import io.restassured.RestAssured
import io.restassured.http.ContentType
import ncei.catalog.Application
import ncei.catalog.domain.MetadataSchema
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cassandra.core.cql.CqlIdentifier
import org.springframework.data.cassandra.core.CassandraAdminOperations
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.hamcrest.Matchers.equalTo
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [Application], webEnvironment = RANDOM_PORT)
class SchemaApiSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  @Autowired
  RabbitTemplate rabbitTemplate

  PollingConditions poller

  @Autowired
  private CassandraAdminOperations adminTemplate

  final static String DATA_TABLE_NAME = 'MetadataSchema'

  def setup() {
    poller = new PollingConditions(timeout: 10)
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath

    adminTemplate.createTable(
        true, CqlIdentifier.cqlId(DATA_TABLE_NAME),
         MetadataSchema.class, new HashMap<String, Object>())
  }

  public static final String KEYSPACE_CREATION_QUERY = "CREATE KEYSPACE IF NOT EXISTS metacat WITH replication = { 'class': 'SimpleStrategy', 'replication_factor': '3' };"

  public static final String KEYSPACE_ACTIVATE_QUERY = "USE metacat;"

  def setupSpec() {
    final Cluster cluster = Cluster.builder().addContactPoints("127.0.0.1").withPort(9042).build()
    final Session session = cluster.connect()
    session.execute(KEYSPACE_CREATION_QUERY)
    session.execute(KEYSPACE_ACTIVATE_QUERY)
    Thread.sleep(5000)
  }

  def cleanup() {
    adminTemplate.truncate(DATA_TABLE_NAME)//dropTable(CqlIdentifier.cqlId(DATA_TABLE_NAME))
  }

  def 'create, read, update, delete schema metadata'() {
    setup: 'define a schema metadata record'
    def postBody = [
            "schema_name": "schemaFace",
            "json_schema": "{blah:blah}"
    ]

    when: 'we post, a new record is create and returned in response'
    Map schemaMetadata = RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
            .when()
            .post('/schemas')
            .then()
            .assertThat()
            .statusCode(201)
            .body('data[0].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[0].attributes.json_schema', equalTo(postBody.json_schema))
            .extract()
            .path('data[0].attributes')

    then: 'we can get it by id'
    RestAssured.given()
            .contentType(ContentType.JSON)
            .when()
            .get("/schemas/${schemaMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('data[0].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[0].attributes.json_schema', equalTo(postBody.json_schema))


    when: 'we update the postBody with the id and new metadata'

    String updatedSchema = "different schema"
    Map updatedPostBody = schemaMetadata.clone()
    updatedPostBody.json_schema = updatedSchema

    then: 'we can update it (create a new version)'

    RestAssured.given()
            .body(updatedPostBody)
            .contentType(ContentType.JSON)
            .when()
            .put("/schemas/${schemaMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('data[0].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[0].attributes.json_schema', equalTo(updatedSchema))

    and: 'we can get both versions'
    Map updatedRecord = RestAssured.given()
            .param('showVersions', true)
            .when()
            .get("/schemas/${schemaMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('meta.totalResults', equalTo(2))
    //first one is the newest
            .body('data[0].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[0].attributes.json_schema', equalTo(updatedSchema))
    //second one is the original
            .body('data[1].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[1].attributes.json_schema', equalTo(postBody.json_schema))
            .extract()
            .path('data[0].attributes')

    then: 'submit the latest schema back with a delete method to delete it'
    //delete it
    RestAssured.given()
            .body(updatedRecord)
            .contentType(ContentType.JSON)
            .when()
            .delete("/schemas/${schemaMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('meta.message' as String, equalTo('Successfully deleted row with id: ' + updatedPostBody.id))

    and: 'it is gone, but we can get it with a a flag- showDeleted'
    RestAssured.given()
            .when()
            .get("/schemas/${schemaMetadata.id}")
            .then()
            .assertThat()
            .contentType(ContentType.JSON)
            .statusCode(404)  //should be a 404
            .body('data', equalTo(null))
            .body('errors', equalTo(['No results found.']))

    RestAssured.given()
            .param('showDeleted', true)
            .when()
            .get("/schemas/${schemaMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('meta.totalResults', equalTo(1))
            .body('data[0].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[0].attributes.json_schema', equalTo(updatedSchema))
            .body('data[0].attributes.deleted', equalTo(true))

    and: 'we can get all 3 back with showDeleted AND showVersions'
    RestAssured.given()
            .param('showDeleted', true)
            .param('showVersions', true)
            .when()
            .get("/schemas/${schemaMetadata.id}")
            .then()
            .assertThat()
            .statusCode(200)
            .body('meta.code', equalTo(200))
            .body('meta.success', equalTo(true))
            .body('meta.action', equalTo('read'))
            .body('meta.totalResults', equalTo(3))
            .body('data[0].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[0].attributes.json_schema', equalTo(updatedSchema))
            .body('data[0].attributes.deleted', equalTo(true))

            .body('data[1].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[1].attributes.json_schema', equalTo(updatedSchema))

            .body('data[2].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[2].attributes.json_schema', equalTo(postBody.json_schema))

    then: 'clean up the db, purge all 3 records by id'
    //delete all with that id
    RestAssured.given()
            .body(updatedRecord) //id in here
            .contentType(ContentType.JSON)
            .when()
            .delete('/schemas/purge')
            .then()
            .assertThat()
            .statusCode(200)
            .body('meta.id', equalTo(updatedRecord.id))
            .body('meta.totalResultsDeleted', equalTo(3))
            .body('meta.success', equalTo(true))

            .body('data[1].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[1].attributes.json_schema', equalTo(updatedSchema))

            .body('data[2].attributes.schema_name', equalTo(postBody.schema_name))
            .body('data[2].attributes.json_schema', equalTo(postBody.json_schema))

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
