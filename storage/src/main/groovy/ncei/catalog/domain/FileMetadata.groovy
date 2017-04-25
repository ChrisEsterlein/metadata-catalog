package ncei.catalog.domain

class FileMetadata {
  String trackingId
  String filename
  String dataset
  String type
  Integer fileSize
  String fileMetadata
  String geometry

  Map asMap() {
    this.class.declaredFields.findAll {
      !it.synthetic }.collectEntries {
        [ (it.name):this."$it.name" ]
      }
  }

}