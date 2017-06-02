package ncei.catalog.filters.utils

import org.codehaus.jettison.json.JSONObject
import org.springframework.stereotype.Component

@Component
class RequestConversionUtil {

  JSONObject transformRecorderPost(Map legacyPostBody) {

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
    granulePostBody as JSONObject
  }

  JSONObject transformRecorderResponse(Map jsonApiResponseBody, int code) {
    Map legacyResonse = [:]
    legacyResonse.items = []
    legacyResonse.code = code
    legacyResonse.totalResultsUpdated = jsonApiResponseBody.data.size
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
    legacyResonse as JSONObject
  }
}
