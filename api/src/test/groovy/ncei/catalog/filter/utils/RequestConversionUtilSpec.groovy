package ncei.catalog.filter.utils

import ncei.catalog.filters.utils.RequestConversionUtil
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class RequestConversionUtilSpec extends Specification {

  def 'legacy transform of null post body works'() {

    expect:
    RequestConversionUtil.transformLegacyPostBody(null) == "{}"
  }

  def 'legacy transform of response body #description works'() {

    expect:
    RequestConversionUtil.transformLegacyGetResponse((Map) responseBody, code) == expResponse

    where:
    description                                   | responseBody | code | expResponse
    "that's empty"                                | [:]          | 1    | '{"dataset":"null","items":[],"totalResults":null,"searchTerms":null,"code":1}'
    "that's missing totalResults and searchTerms" | [meta: [:]]  | 1    | '{"dataset":"null","items":[],"totalResults":null,"searchTerms":null,"code":1}'
  }

  def 'legacy transform of GET params #params transforms as expected'() {

    expect:
    RequestConversionUtil.transformParams(params) == expTransformedParams

    where:
    params                                        | expTransformedParams
    null                                          | [:]
    [:]                                           | [:]
    [dataset: ["csb"], max: ["0"], offset: ["0"]] | [q: ["dataset:csb"], max: ["0"], offset: ["0"]] // This is the only supported format
  }

  def 'legacy acquiring of dataset from query string of "#queryString" works'() {

    expect:
    RequestConversionUtil.getDatasetFromQueryString(queryString) == expDataset
    where:
    queryString                      | expDataset
    null                             | "null"
    'dataset: csb'                   | 'csb'
    'dataset:csb'                    | 'csb'
    ' dataset: csb'                  | 'csb' // Unsure possible via elasticsearch simple query
  }
}
