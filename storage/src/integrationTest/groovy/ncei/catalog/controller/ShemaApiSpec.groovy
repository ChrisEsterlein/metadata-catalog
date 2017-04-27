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
class ShemaApiSpec extends Specification{

    @Value('${local.server.port}')
    private String port

    @Value('${server.context-path:/}')
    private String contextPath

    def setup() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port as Integer
        RestAssured.basePath = contextPath
    }

    def 'create, read, update, delete schema metadata'(){
        setup: 'define a schema metadata record'
        def postBody = [
                "schema_name":"schemaFace",
                "schema_json":"a metadata schema"
        ]

        when: 'we post, a new record is create and returned in response'
        RestAssured.given()
                .body(postBody)
                .contentType(ContentType.JSON)
            .when()
                .post('/schemas/create')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
                .body('newRecord.schema_name', equalTo(postBody.schema_name))
                .body('newRecord.json_schema', equalTo(postBody.json_schema))

        then: 'we can select it back out to get the schema_id'
        String schema_id = RestAssured.given()
                .contentType(ContentType.JSON)
            .when()
                .get('/schemas')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
                .body('schemas[0].schema_name', equalTo(postBody.schema_name))
                .body('schemas[0].json_schema', equalTo(postBody.json_schema))
            .extract()
                .path('schemas[0].schema_id')

        when: 'we update the postBody with the schema_id and new metadata'

        String updatedMetadata = "different metadata"
        Map updatedPostBody = postBody.clone() as Map
        updatedPostBody.json_schema = updatedMetadata
        updatedPostBody.schema_id = schema_id

        then: 'we can update it (create a new version)'
        RestAssured.given()
                .body(updatedPostBody)
                .contentType(ContentType.JSON)
            .when()
                .put('/schemas/update')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
                .body('newRecord.schema_name', equalTo(postBody.schema_name))
                .body('newRecord.json_schema', equalTo(updatedMetadata))

        and: 'we can get both versions'
        RestAssured.given()
                .param('dataset', 'test')
                .param('versions', true)
            .when()
                .get('/schemas')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
        //first one is the newest
                .body('schemas[0].schema_name', equalTo(postBody.schema_name))
                .body('schemas[0].json_schema', equalTo(updatedMetadata))
        //second one is the original
                .body('schemas[1].schema_name', equalTo(postBody.schema_name))
                .body('schemas[1].json_schema', equalTo(postBody.json_schema))

        then: 'submit the delete body to delete the latest version'
        def deleteBody = [schema_id: schema_id]

        //delete it
        RestAssured.given()
                .body(deleteBody)
                .contentType(ContentType.JSON)
            .when()
                .delete('/schemas/delete')
            .then()
                .assertThat()
                .statusCode(200)
                .body('message' as String, equalTo('Successfully deleted row with schema_id: ' + schema_id))

        and: 'we can select the previous version back out'
        RestAssured.given()
            .when()
                .get('/schemas')
            .then()
                .assertThat()
                .contentType(ContentType.JSON)
                .statusCode(200)  //should be a 201
                .body('schemas[0].filename', equalTo(postBody.filename) )
                .body('schemas[0].json_schema', equalTo(postBody.json_schema))

        then: 'we can delete the original version too'
        //delete all with that schema_id
        RestAssured.given()
                .body(deleteBody)
                .contentType(ContentType.JSON)
            .when()
                .delete('/schemas/purge')
            .then()
                .assertThat()
                .statusCode(200)
                .body('message' as String, equalTo('Successfully purged 1 rows matching ' + deleteBody))
    }
}
