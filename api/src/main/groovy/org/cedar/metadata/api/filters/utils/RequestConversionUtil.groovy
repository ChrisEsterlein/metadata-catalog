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
          granulePostBody.id = value
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
    return (granulePostBody.sort() as JSONObject) as String
  }

  static String transformLegacyGetResponse(Map jsonApiResponseBody, int code) {
    Map legacyResponse = [:]
    legacyResponse.items = []
    legacyResponse.code = code

    legacyResponse.dataset = getDatasetFromQueryString(jsonApiResponseBody?.meta?.searchTerms?.q)
    legacyResponse.searchTerms = getSearchTerms(jsonApiResponseBody?.meta?.searchTerms, legacyResponse.dataset)

    jsonApiResponseBody?.data?.each {
      Map item = [:]
      it?.attributes?.each { key, value ->
        switch (key) {
          case 'id':
            item.trackingId = value as String
            break
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
      item ? legacyResponse.items << item.sort() : ''
    }
    legacyResponse.totalResults = legacyResponse?.items?.size

    return (legacyResponse.sort() as JSONObject) as String
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
    return querySplit.length > 1 ? querySplit[1] : "null"
  }

  static def getSearchTerms(Map responseSearchTerms, def dataset) {
    Map searchTerms = [:]
    responseSearchTerms ? searchTerms = responseSearchTerms : ''
    responseSearchTerms?.from != null ? searchTerms.offset = searchTerms.remove('from') : ''
    responseSearchTerms?.size != null ? searchTerms.max = searchTerms.remove('size') : ''
    dataset != 'null' ? searchTerms.dataset = dataset : ''
    return searchTerms
  }
}
