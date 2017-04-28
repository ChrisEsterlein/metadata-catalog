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
class CollectionApiSpec extends Specification{

    @Value('${local.server.port}')
    private String port

    @Value('${server.context-path:/}')
    private String contextPath

    def setup() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port as Integer
        RestAssured.basePath = contextPath
    }

    def 'create, read, update, delete collection metadata'(){
        setup: 'define a collection metadata record'
        def postBody = [
            "collection_name":"collectionFace",
            "collection_schema":"a collection schema",
            "type":"fos",
            "collection_metadata":"{blah:blah}"
        ]

        when: 'we post, a new record is create and returned in response'
        RestAssured.given()
                .body(postBody)
                .contentType(ContentType.JSON)
            .when()
                .post('/collections/create')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
                .body('newRecord.collection_name', equalTo(postBody.collection_name))
                .body('newRecord.collection_size', equalTo(postBody.collection_size))
                .body('newRecord.collection_metadata', equalTo(postBody.collection_metadata))
                .body('newRecord.geometry', equalTo(postBody.geometry))
                .body('newRecord.type', equalTo(postBody.type))

        then: 'we can select it back out to get the collection_id'
        Map collectionMetdata = RestAssured.given()
                .contentType(ContentType.JSON)
            .when()
                .get('/collections')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
                .body('collections[0].collection_name', equalTo(postBody.collection_name))
                .body('collections[0].collection_size', equalTo(postBody.collection_size))
                .body('collections[0].collection_metadata', equalTo(postBody.collection_metadata))
                .body('collections[0].geometry', equalTo(postBody.geometry))
                .body('collections[0].type', equalTo(postBody.type))
            .extract()
                .path('collections[0]')

        when: 'we update the postBody with the collection_id and new metadata'

        String updatedMetadata = "different metadata"
        Map updatedPostBody = collectionMetdata.clone()
        updatedPostBody.collection_metadata = updatedMetadata

        then: 'we can update it (create a new version)'
        RestAssured.given()
                .body(updatedPostBody)
                .contentType(ContentType.JSON)
            .when()
                .put('/collections/update')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
                .body('newRecord.collection_name', equalTo(postBody.collection_name))
                .body('newRecord.collection_size', equalTo(postBody.collection_size))
                .body('newRecord.collection_metadata', equalTo(updatedMetadata))
                .body('newRecord.geometry', equalTo(postBody.geometry))
                .body('newRecord.type', equalTo(postBody.type))

        and: 'we can get both versions'
        RestAssured.given()
                .param('dataset', 'test')
                .param('versions', true)
            .when()
                .get('/collections')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
        //first one is the newest
                .body('collections[0].collection_name', equalTo(postBody.collection_name))
                .body('collections[0].collection_size', equalTo(postBody.collection_size))
                .body('collections[0].collection_metadata', equalTo(updatedMetadata))
                .body('collections[0].geometry', equalTo(postBody.geometry))
                .body('collections[0].type', equalTo(postBody.type))
        //second one is the original
                .body('collections[1].collection_name', equalTo(postBody.collection_name))
                .body('collections[1].collection_size', equalTo(postBody.collection_size))
                .body('collections[1].collection_metadata', equalTo( postBody.collection_metadata))
                .body('collections[1].geometry', equalTo(postBody.geometry))
                .body('collections[1].type', equalTo(postBody.type))

        then: 'submit the delete body to delete the latest version'
        def deleteBody = [collection_id: updatedPostBody.collection_id]

        //delete it
        RestAssured.given()
                .body(deleteBody)
                .contentType(ContentType.JSON)
            .when()
                .delete('/collections/delete')
            .then()
                .assertThat()
                .statusCode(200)
                .body('message' as String, equalTo('Successfully deleted row with collection_id: ' + updatedPostBody.collection_id))

        and: 'we can select the previous version back out'
        RestAssured.given()
            .when()
                .get('/collections')
            .then()
                .assertThat()
                .contentType(ContentType.JSON)
                .statusCode(200)  //should be a 201
                .body('collections[0].filename', equalTo(postBody.filename) )
                .body('collections[0].collection_size', equalTo(postBody.collection_size))
                .body('collections[0].collection_metadata', equalTo(postBody.collection_metadata)) //notice this is the old data
                .body('collections[0].dataset', equalTo(postBody.dataset))
                .body('collections[0].geometry', equalTo(postBody.geometry))

        then: 'we can delete the original version too'
        //delete all with that granule_id
        RestAssured.given()
                .body(deleteBody)
                .contentType(ContentType.JSON)
            .when()
                .delete('/collections/purge')
            .then()
                .assertThat()
                .statusCode(200)
                .body('message' as String, equalTo('Successfully purged 1 rows matching ' + deleteBody))
    }
}
