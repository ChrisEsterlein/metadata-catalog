package ncei.catalog

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.hamcrest.Matchers.hasItems
import static org.hamcrest.Matchers.equalTo
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [ApiApplication], webEnvironment = RANDOM_PORT)
class SaveDataAPISpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  @Shared
  private String appUrl

//  def poller = new PollingConditions(timeout: 5)

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
    appUrl = "${RestAssured.baseURI}:${RestAssured.port}/api"
  }

  @Unroll
  def 'save new granules with #trackingId and #filename'() {
    when: 'posting a new granule to the storage'
    String id = RestAssured.given()
        .body([
          "tracking_id": trackingId,
          "filename": filename
        ])
        .contentType(ContentType.JSON)
        .when()
        .post("${appUrl}/${route}/${path}")
        .then()
        .assertThat()
        .statusCode(201)
        .extract()
        .path('data[0].id')

    then: 'id returned is not null'
    id != null

    and: 'eventually searching the index finds the record'
    /*
    poller.eventually {

      sleep(500)

      def result = RestAssured.given()
          .contentType(ContentType.JSON)
          .params([q: "_id:$id"])
          .when()
          .get("${appUrl}/index/index/search")
          .then()
          .extract()
//      assert result.response().statusCode == 200
      assert result.body().path("data.id").contains(id)
    }
    // */

    sleep(1000)
    RestAssured.given()
        .contentType(ContentType.JSON)
        .params([q: "_id:$id"])
        .when()
        .get("${appUrl}/index/index/search")
        .then()
        .assertThat()
        .statusCode(200)
        .body("data.size", equalTo(1))
        .body("data.id", hasItems(id))

    where:
    route | path | trackingId | filename
    'storage1' | 'metadata-catalog/granules' | "abc123" | "some_granule.txt"
    'storage1' | 'metadata-catalog/granules' | "xyz789" | "a_granule.zip"
  }

}
