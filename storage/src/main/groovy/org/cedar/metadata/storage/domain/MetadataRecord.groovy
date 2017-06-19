package org.cedar.metadata.storage.domain

abstract class MetadataRecord implements MetadataRecordInterface {

  Map asMap() {
    this.class.declaredFields.findAll {
      !it.synthetic
    }.collectEntries {
      [(it.name): this."$it.name"]
    }
  }
}
