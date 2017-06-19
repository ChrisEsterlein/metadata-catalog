package org.cedar.metadata.storage.domain

//Parent class to Collections, Granules, Schemas
interface MetadataRecordInterface {

  Map asMap()

  String recordTable()

}
