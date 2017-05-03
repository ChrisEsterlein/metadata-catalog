package ncei.catalog.domain

class FileMetadata {
  String trackingId
  String last_update
  String filename
  String dataset
  String type
  String accessProtocol
  Integer fileSize
  String fileMetadata
  String geometry
  Boolean deleted

  Map asMap() {
    this.class.declaredFields.findAll {
      !it.synthetic }.collectEntries {
        [ (it.name):this."$it.name" ]
      }
  }

}