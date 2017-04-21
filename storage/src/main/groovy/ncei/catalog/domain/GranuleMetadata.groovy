package ncei.catalog.domain

import com.datastax.driver.core.utils.UUIDs
import org.springframework.cassandra.core.Ordering
import org.springframework.cassandra.core.PrimaryKeyType
import org.springframework.data.cassandra.mapping.Indexed
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.mapping.Table

@Table(value= 'GranuleMetadata')
class GranuleMetadata {

  @PrimaryKeyColumn(name = "metadata_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
  UUID metadata_id

  @PrimaryKeyColumn(name = "last_update", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
  Date last_update

  @Indexed
  String dataset

  @Indexed
  String granule_schema

  String tracking_id
  String filename
  String type
  Integer granule_size
  String granule_metadata
  String geometry
  List collection

  GranuleMetadata() {
    this.metadata_id = UUIDs.timeBased()
    this.last_update = new Date()
  }

}