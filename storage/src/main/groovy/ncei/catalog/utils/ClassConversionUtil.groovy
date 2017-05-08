package ncei.catalog.utils

import ncei.catalog.domain.FileMetadata
import ncei.catalog.domain.GranuleMetadata


class ClassConversionUtil {

  static GranuleMetadata convertToGranuleMetadata(FileMetadata fileMetadata) {
    GranuleMetadata granuleMetadata = new GranuleMetadata()

    fileMetadata.properties.each { key, value ->
      switch (key) {
        case 'class':
          break
        case 'trackingId':
          granuleMetadata.tracking_id = value
          break
        case 'fileSize':
          granuleMetadata.granule_size = value as Integer
          break
        case 'fileMetadata':
          granuleMetadata.granule_metadata = value
          break
        case 'accessProtocol':
          granuleMetadata.access_protocol = value
          break
        default:
          if (granuleMetadata.hasProperty(key)) {
            granuleMetadata[key] = value
          }
          break
      }
    }
    granuleMetadata
  }

  static FileMetadata convertToFileMetadata(GranuleMetadata granuleMetadata) {
    FileMetadata fileMetadata = new FileMetadata()

    granuleMetadata.properties.each { key, value ->
      switch (key) {
        case 'class':
          break
        case 'granule_metadata':
          fileMetadata.fileMetadata = value
          break
        case 'tracking_id':
          fileMetadata.trackingId = value
          break
        case 'granule_size':
          fileMetadata.fileSize = value as Integer
          break
        case 'access_protocol':
          fileMetadata.accessProtocol = value
          break
        default:
          if (fileMetadata.hasProperty(key)) {
            fileMetadata[key] = value
          }
          break
      }
    }
    fileMetadata
  }

}
