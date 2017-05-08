package ncei.catalog.domain

//Parent class to Collections, Granules, Schemas
class MetadataRecord {

  Map asMap() {
    this.class.declaredFields.findAll {
      !it.synthetic
    }.collectEntries {
      [(it.name): this."$it.name"]
    }
  }
}
