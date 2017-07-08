package org.cedar.metadata.storage.domain

import com.datastax.driver.core.utils.UUIDs
import org.springframework.cassandra.core.Ordering
import org.springframework.cassandra.core.PrimaryKeyType
import org.springframework.data.cassandra.mapping.Indexed
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.mapping.Table

@Table(value = 'CollectionMetadata')
class CollectionMetadata extends MetadataRecord {

  @PrimaryKeyColumn(name = "id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
  UUID id

  @PrimaryKeyColumn(name = "last_update", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
  Date last_update

  @Indexed
  String name

  @Indexed
  String metadata_schema

  String submission_id
  String type
  String metadata
  String geometry
  Boolean deleted

  CollectionMetadata() {
    this.id = UUIDs.timeBased()
    this.last_update = new Date()
    this.deleted = false
  }

  String recordTable() {
    return 'collection'
  }

}
