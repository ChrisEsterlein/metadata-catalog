package ncei.catalog.filters.utils

import org.codehaus.jettison.json.JSONObject

class RequestConversionUtil {

  static String transformLegacyMetadataRecorderPostBody(Map legacyPostBody) {

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

  static String transformResponse(Map jsonApiResponseBody, int code) {
    Map legacyResonse = [:]
    legacyResonse.items = []
    legacyResonse.code = code
    legacyResonse.totalResultsUpdated = jsonApiResponseBody?.data?.size
    jsonApiResponseBody.data.each {
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
      legacyResonse.items << item
    }
    return (legacyResonse as JSONObject) as String
  }

  static Map<String, List<String>> transformParams(Map<String, List<String>> params = [:]) {
    Map newParams = params ? params.subMap(['max', 'offset']) : [:]
    params ? params -= newParams : ''
    List<String> qValue = params ? [params.collect({ k, v -> "$k:${v[0]}" }).join(' AND ')].toList() : null

    qValue ? newParams.q = qValue : ''

    return newParams
  }
}
