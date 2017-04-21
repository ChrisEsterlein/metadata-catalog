package ncei.catalog.utils

import ncei.catalog.domain.FileMetadata
import ncei.catalog.domain.GranuleMetadata


class ClassConversionUtil {

  static GranuleMetadata convertToGranuleMetadata(FileMetadata fileMetadata){
    GranuleMetadata granuleMetadata = new GranuleMetadata()

    fileMetadata.properties.each{key, value ->
      switch (key){
        case 'class':
          break
        case 'trackingId':
          granuleMetadata.tracking_id = value
          break
        case 'file_size':
          granuleMetadata.granule_size = value as Integer
          break
        default:
          granuleMetadata."${key}" = value
          break
      }
    }
    granuleMetadata
  }

  static FileMetadata convertToFileMetadata(GranuleMetadata granuleMetadata){
    FileMetadata fileMetadata = new FileMetadata()
    List keysNotFileMetadata = ['granule_id', 'last_update', 'granule_schema', 'collection']

    granuleMetadata.properties.each{key, value ->
      switch (key){
        case 'class':
          break
        case 'granule_metadata':
          fileMetadata.fileMetadata = value
          break
        case 'tracking_id':
          fileMetadata.trackingId = value
          break
        case 'granule_size':
          fileMetadata.file_size = value as Integer
          break
        case (!(key in keysNotFileMetadata) ):
          fileMetadata."${key}" = value
          break
      }
    }

    fileMetadata
  }

}
