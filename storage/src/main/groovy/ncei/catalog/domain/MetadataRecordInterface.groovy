package ncei.catalog.domain

//Parent class to Collections, Granules, Schemas
interface MetadataRecordInterface {

  Map asMap()
  String recordTable()

}