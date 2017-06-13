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

  def 'legacy transform of response body #description as a #method works'() {

    expect:
    RequestConversionUtil.transformLegacyResponse((Map) responseBody, code, method) == expResponse

    where:
    description                       | responseBody                       | code | method | expResponse
    "that's empty"                    | [:]                                | 1    | 'GET'  | '{"dataset":"null","items":[],"searchTerms":null,"code":1,"totalResults":null}'
    "that's meta is empty"            | [meta: [:]]                        | 1    | 'GET'  | '{"dataset":"null","items":[],"searchTerms":null,"code":1,"totalResults":null}'
    "that's data is empty"            | [data: [:]]                        | 1    | 'GET'  | '{"dataset":"null","items":[],"searchTerms":null,"code":1,"totalResults":null}'
    "that's data is empty"            | [data: [:]]                        | 1    | 'POST' | '{"dataset":"null","items":[],"searchTerms":null,"code":1,"totalResultsUpdated":null}'
    "that's missing q"                | [meta: [searchTerms: [:]]]         | 1    | 'GET'  | '{"dataset":"null","items":[],"searchTerms":{},"code":1,"totalResults":null}'
    "that's missing totalResults for" | [meta: [searchTerms: [q: "blah"]]] | 1    | 'GET'  | '{"dataset":"null","items":[],"searchTerms":{"q":"blah"},"code":1,"totalResults":null}'
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
    queryString     | expDataset
    null            | "null"
    'dataset: csb'  | 'csb'
    'dataset:csb'   | 'csb'
    ' dataset: csb' | 'csb' // Unsure if even possible via elasticsearch simple query
  }
}
