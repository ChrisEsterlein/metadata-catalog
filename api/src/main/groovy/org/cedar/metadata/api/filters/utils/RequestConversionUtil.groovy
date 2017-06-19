package org.cedar.metadata.api.filters.utils

import groovy.util.logging.Slf4j
import org.codehaus.jettison.json.JSONObject

@Slf4j
class RequestConversionUtil {

  static String transformLegacyPostBody(Map legacyPostBody) {

    Map granulePostBody = [:]

    legacyPostBody.each { key, value ->
      switch (key) {
        case 'trackingId':
          granulePostBody.tracking_id = value
          break
        case 'fileSize':
          granulePostBody.size_bytes = value
          break
        case 'fileMetadata':
          granulePostBody.metadata = value
          break
        default:
          granulePostBody."${key}" = value
          break
      }
    }
    return (granulePostBody as JSONObject) as String
  }

  static String transformLegacyResponse(Map jsonApiResponseBody, int code, String method) {
    Map legacyResponse = [:]
    legacyResponse.dataset = getDatasetFromQueryString(jsonApiResponseBody?.meta?.searchTerms?.q)
    legacyResponse.items = []
    legacyResponse.searchTerms = jsonApiResponseBody?.meta?.searchTerms
    legacyResponse.code = code

    if (method.equals('GET')) {
      legacyResponse.totalResults = jsonApiResponseBody?.meta?.totalResults
    } else {
      legacyResponse.totalResultsUpdated = jsonApiResponseBody?.data?.size
    }

    jsonApiResponseBody?.data?.each {
      Map item = [:]
      it.attributes.each { key, value ->
        switch (key) {
          case 'size_bytes':
            item.fileSize = value as Integer
            break
          case 'metadata':
            item.fileMetadata = value as String
            break
          default:
            item."${key.replaceAll(/_\w/) { it[1].toUpperCase() }}" = value
            break
        }
      }
      legacyResponse.items << item
    }
    return (legacyResponse as JSONObject) as String
  }

  static Map<String, List<String>> transformParams(Map<String, List<String>> params = [:]) {
    Map newParams = params ? params.subMap(['max', 'offset']) : [:]
    params ? params -= newParams : ''
    List<String> qValue = params ? [params.collect({ k, v -> "$k:${v[0]}" }).join(' AND ')].toList() : null

    qValue ? newParams.q = qValue : ''

    return newParams
  }

  /**
   * Only supported format for queryString via legacy endpoint is "dataset:blah" (with spaces inserted is ok) or no dataset.
   * @param queryString
   * @return Dataset found in query params.
   */
  static String getDatasetFromQueryString(String queryString) {
    String[] querySplit = queryString ? queryString.trim().split('dataset[\\s]*:[\\s]*') : []
    querySplit.each { System.out.println('value:' + it) }
    return querySplit.length > 1 ? querySplit[1] : "null"
  }
}
