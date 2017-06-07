package ncei.catalog.filter.utils

import ncei.catalog.filters.utils.RequestConversionUtil
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class RequestConversionUtilSpec extends Specification {

  def 'legacy transform of null post body works'() {

    expect:
    RequestConversionUtil.transformLegacyMetadataRecorderPostBody(null) == "{}"
  }

  def 'legacy transform of response body #description works'() {

    expect:
    RequestConversionUtil.transformResponse((Map) responseBody, code) == expResponse

    where:
    description       | responseBody | code | expResponse
    "missing data"    | [:]          | 1    | '{"items":[],"code":1,"totalResultsUpdated":null}'
    "with empty data" | [data: []]   | 1    | '{"items":[],"code":1,"totalResultsUpdated":0}'
  }

  def 'legacy transform of GET params #params transforms as expected'() {

    expect:
    RequestConversionUtil.transformParams(params) == expTransformedParams

    where:
    params                                        | expTransformedParams
    null                                          | [:]
    [:]                                           | [:]
    [dataset: ["csb"], filename: ["test"]]        | [q: ["dataset:csb AND filename:test"]]
    [offset: ["0"]]                               | [offset: ["0"]]
    [max: ["0"]]                                  | [max: ["0"]]
    [max: ["0"], offset: ["0"]]                   | [max: ["0"], offset: ["0"]]
    [dataset: ["csb"], max: ["0"], offset: ["0"]] | [q: ["dataset:csb"], max: ["0"], offset: ["0"]]
  }
}
