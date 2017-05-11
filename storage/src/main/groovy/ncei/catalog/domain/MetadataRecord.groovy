package ncei.catalog.domain

import com.datastax.driver.core.utils.UUIDs

//Parent class to Collections, Granules, Schemas
abstract class MetadataRecord {

//  UUID id
//  Date last_update //as Long
//  Boolean deleted
//
//  MetadataRecord(){
//    this.id = UUIDs.timeBased()
//    this.last_update = new Date()
//    this.deleted = false
//  }

  Map asMap() {
    this.class.declaredFields.findAll {
      !it.synthetic
    }.collectEntries {
      [(it.name): this."$it.name"]
    }
  }
}
