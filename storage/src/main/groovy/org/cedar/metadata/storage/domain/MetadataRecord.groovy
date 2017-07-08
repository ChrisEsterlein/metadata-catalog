package org.cedar.metadata.storage.domain

abstract class MetadataRecord implements MetadataRecordInterface {

  Map asMap() {
    this.class.declaredFields.findAll {
      !it.synthetic
    }.collectEntries {
      [(it.name): this."$it.name"]
    }
  }

  MetadataRecord leftShift(MetadataRecord mr) {
    Map overrides = mr.asMap()
    this.class.declaredFields.findAll {
      !it.synthetic
    }.each{
      if(overrides."${it.name}"){
        this.setProperty(it.name, overrides."${it.name}")
      }
    }
    this
  }

  @Override
  String toString() {
    return asMap()
  }
}
