package ncei.catalog.filters.utils

import org.springframework.stereotype.Component

@Component
class FilterHelper {

  Map transformRecorderPost(Map postBody){
    return convertToGranuleMetadata(postBody)
  }

  Map convertToGranuleMetadata(Map fileMetadata){

    Map granuleMetadata = [:]

    fileMetadata.each{key, value ->
      switch (key){
        case 'class':
          break
        case 'trackingId':
          granuleMetadata.tracking_id = value
          break
        case 'fileSize':
          granuleMetadata.granule_size = value as Integer
          break
        case 'fileMetadata':
          granuleMetadata.granule_metadata = value as String
          break
        default:
          granuleMetadata."${key}" = value
          break
      }
    }
    granuleMetadata
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
