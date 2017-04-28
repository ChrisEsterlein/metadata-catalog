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
class GranuleApiSpec extends Specification{

    @Value('${local.server.port}')
    private String port

    @Value('${server.context-path:/}')
    private String contextPath

    def setup() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port as Integer
        RestAssured.basePath = contextPath
    }

    def 'create, read, update, delete granule metadata'(){
        setup: 'define a granule metadata record'
        def postBody = [
                "dataset": "test",
                "granule_schema": "a granule schema",
                "tracking_id": "ABCD",
                "filename": "myfile",
                "type": "data",
                "access_protocol": "FILE",
                "granule_size": 42,
                "granule_metadata": "this is some raw scraped metadata from a header or whatever",
                "geometry": "POLYGON((0 0) (0 1) (1 1) (1 0))",
                "collections": ["FOS"]
        ]

        when: 'we post, a new record is create and returned in response'
        RestAssured.given()
            .body(postBody)
            .contentType(ContentType.JSON)
        .when()
            .post('/granules/create')
        .then()
            .assertThat()
            .statusCode(200)  //should be a 201
            .body('newRecord.filename', equalTo(postBody.filename))
            .body('newRecord.granule_size', equalTo(postBody.granule_size))
            .body('newRecord.granule_metadata', equalTo(postBody.granule_metadata))
            .body('newRecord.dataset', equalTo(postBody.dataset))
            .body('newRecord.geometry', equalTo(postBody.geometry))

        then: 'we can select it back out and get the granule_id' //granule_id is also returned in post response
        Map granuleMetdata = RestAssured.given()
                .contentType(ContentType.JSON)
            .when()
                .get('/granules')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
                .body('granules[0].filename', equalTo(postBody.filename) )
                .body('granules[0].granule_size', equalTo(postBody.granule_size))
                .body('granules[0].granule_metadata', equalTo(postBody.granule_metadata))
                .body('granules[0].dataset', equalTo(postBody.dataset))
                .body('granules[0].geometry', equalTo(postBody.geometry))
            .extract()
                .path('granules[0]')

        when: 'we update the postBody with the granule_id and new metadata'

        String updatedMetadata = "different metadata"
        Map updatedPostBody = granuleMetdata.clone() as Map
        updatedPostBody.granule_metadata = updatedMetadata

        then: 'we can update it (create a new version)'
        RestAssured.given()
                .body(updatedPostBody)
                .contentType(ContentType.JSON)
            .when()
                .put('/granules/update')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
                .body('newRecord.filename', equalTo(postBody.filename))
                .body('newRecord.granule_size', equalTo(postBody.granule_size))
                .body('newRecord.granule_metadata', equalTo(updatedMetadata))
                .body('newRecord.dataset', equalTo(postBody.dataset))
                .body('newRecord.geometry', equalTo(postBody.geometry))

        and: 'we can get both version'
        RestAssured.given()
                .param('dataset', 'test')
                .param('versions', true)
            .when()
                .get('/granules')
            .then()
                .assertThat()
                .statusCode(200)  //should be a 201
        //first one is the newest
                .body('granules[0].filename', equalTo(postBody.filename) )
                .body('granules[0].granule_size', equalTo(postBody.granule_size))
                .body('granules[0].granule_metadata', equalTo(updatedMetadata))
                .body('granules[0].dataset', equalTo(postBody.dataset))
                .body('granules[0].geometry', equalTo(postBody.geometry))
        //second one is the original
                .body('granules[1].filename', equalTo(postBody.filename) )
                .body('granules[1].granule_size', equalTo(postBody.granule_size))
                .body('granules[1].granule_metadata', equalTo(postBody.granule_metadata))
                .body('granules[1].dataset', equalTo(postBody.dataset))
                .body('granules[1].geometry', equalTo(postBody.geometry))


        then: 'submit the delete body to delete the latest version'
        def deleteBody = [granule_id: updatedPostBody.granule_id]

        //delete it
        RestAssured.given()
                .body(deleteBody)
                .contentType(ContentType.JSON)
            .when()
                .delete('/delete')
            .then()
                .assertThat()
                .statusCode(200)
                .body('message' as String, equalTo('Successfully deleted row with granule_id: ' + updatedPostBody.granule_id))

        and: 'we can select the previous version back out'
        RestAssured.given()
            .when()
                .get('/granules')
            .then()
                .assertThat()
                .contentType(ContentType.JSON)
                .statusCode(200)  //should be a 201
                .body('granules[0].filename', equalTo(postBody.filename) )
                .body('granules[0].granule_size', equalTo(postBody.granule_size))
                .body('granules[0].granule_metadata', equalTo(postBody.granule_metadata)) //notice this is the old data
                .body('granules[0].dataset', equalTo(postBody.dataset))
                .body('granules[0].geometry', equalTo(postBody.geometry))

        then: 'we can delete all versions with purge'
        //delete all with that granule_id
        RestAssured.given()
                .body(deleteBody)
                .contentType(ContentType.JSON)
            .when()
                .delete('/granules/purge')
            .then()
                .assertThat()
                .statusCode(200)
                .body('message' as String, equalTo('Successfully purged 1 rows matching ' + deleteBody))
    }

}
