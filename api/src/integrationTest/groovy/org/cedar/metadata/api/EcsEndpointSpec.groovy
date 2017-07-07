package org.cedar.metadata.api

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@Slf4j
@SpringBootTest(classes = [ApiApplication], webEnvironment = RANDOM_PORT)
class EcsEndpointSpec extends Specification {

  @Value('${local.server.port}')
  private String port

  @Value('${server.context-path:/}')
  private String contextPath

  static final String ECS_ENDPOINT = '/ecs-catalog'

  private static controllerDomainFilePairings = [
      ['Velocity', "velocityTestAll.json"],
      ['Velocity', "velocityTestReqd.json"],
      ['SeismicSurveyData', "seismicSurveyDataTestAll.json"],
      ['SeismicSurveyData', "seismicSurveyDataTestReqd.json"],
      ['InterpretedSections', "interpretedSectionsTestAll.json"],
      ['InterpretedSections', "interpretedSectionsTestReqd.json"],
      ['PickValues', "pickValuesTestAll.json"],
      ['PickValues', "pickValuesTestReqd.json"],
      ['SeismicProduct', "seismicProductTestAll.json"],
      ['SeismicProduct', "seismicProductTestReqd.json"],
      ['FosPoint', "fosPointTest01.json"],
      ['Meeting', "meetingTest01.json"],
      ['SedimentThicknessFormula', "sedimentThicknessFormulaTestAll.json"],
      ['SedimentThickness', "sedimentThicknessTestAll.json"]
  ]

  def setup() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = port as Integer
    RestAssured.basePath = contextPath
  }

  @Unroll
  def "create a #domainClass from resource file #resourceFileName"() {
    setup:
    Map postData = new JsonSlurper().parse(new ClassPathResource(resourceFileName as String).getFile())

    expect:
    def response = RestAssured.given()
        .body(postData)
        .contentType(ContentType.JSON)
        .when()
        .post(ECS_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(201)

    log.info response.toString()

    where:
    [domainClass, resourceFileName] << controllerDomainFilePairings
  }
}
