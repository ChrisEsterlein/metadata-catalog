package ncei.catalog.filters.utils

import org.codehaus.jettison.json.JSONObject
import org.springframework.stereotype.Component

@Component
class RequestConversionUtil {

  Map transformRecorderPost(Map postBody){
    return convertToGranuleMetadata(postBody)
  }

  Map convertToGranuleMetadata(Map legacyPostBody){

    Map granulePostBody = [:]

    legacyPostBody.each{key, value ->
      switch (key){
        case 'class':
          break
        case 'trackingId':
          granulePostBody.tracking_id = value
          break
        case 'fileSize':
          granulePostBody.granule_size = value as Integer
          break
        case 'legacyPostBody':
          granulePostBody.granule_metadata = value as String
          break
        default:
          granulePostBody."${key}" = value
          break
      }
    }
    granulePostBody
  }

  JSONObject transformRecorderResponse(Map jsonApiResponseBody){
    Map legacyResonse = [:]
    legacyResonse.items =[]
    jsonApiResponseBody.data.each{
      Map item = [:]
      it.attributes.each{ key, value ->
        switch (key){
          case 'tracking_id':
            item.trackingId = value
            break
          case 'granule_size':
            item.fileSize = value as Integer
            break
          case 'granule_metadata':
            item.file_metadata = value as String
            break
          default:
            item."${key}" = value
            break
        }
      }
      legacyResonse.items << item
    }
    legacyResonse
  }
//
//  static FileMetadata convertToFileMetadata(GranuleMetadata granuleMetadata){
//    FileMetadata fileMetadata = new FileMetadata()
//    List keysNotFileMetadata = ['granule_id', 'last_update', 'granule_schema', 'collection']
//
//    granuleMetadata.properties.each{key, value ->
//      switch (key){
//        case 'class':
//          break
//        case 'granule_metadata':
//          fileMetadata.fileMetadata = value
//          break
//        case 'tracking_id':
//          fileMetadata.trackingId = value
//          break
//        case 'granule_size':
//          fileMetadata.file_size = value as Integer
//          break
//        case (!(key in keysNotFileMetadata) ):
//          fileMetadata."${key}" = value
//          break
//      }
//    }
//
//    fileMetadata
//  }
}
