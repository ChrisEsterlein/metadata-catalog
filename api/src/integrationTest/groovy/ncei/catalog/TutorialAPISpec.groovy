package ncei.catalog

import io.restassured.RestAssured
import io.restassured.response.Response
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(classes = [ApiApplication], webEnvironment = RANDOM_PORT)
class TutorialAPISpec extends Specification{

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  @Shared
  private String appUrl

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
    appUrl = "${RestAssured.baseURI}:${RestAssured.port}/api"
  }

  @Unroll
  def 'test various url configurations #route with path #path'() {
    when:
    Response response = RestAssured.get("${appUrl}/${route}/${path}")

    then:
    response.getStatusCode() == expectedStatus

    where:
    route | path | expectedStatus
    'storage2' | '' | 404 // because there is no data
    'storage1' | 'metadata-catalog/collections' | 404
    'index' | 'index/search' | 200

  }

  def 'test storage unconfigured path'() {
    expect:
    Response response = RestAssured.get("${appUrl}/garbage")
    response.getStatusCode() == 404
  }

  def 'test storage built in (features)'() {
    expect:
    Response response = RestAssured.get("${appUrl}/features")
    response.getStatusCode() == 200
  }

}
