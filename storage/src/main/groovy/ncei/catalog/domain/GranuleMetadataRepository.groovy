package ncei.catalog.domain

import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query

interface GranuleMetadataRepository extends CassandraRepository<GranuleMetadata> {

  @Query("Select * from GranuleMetadata where granule_id =?0 and last_update=?1")
  Iterable<GranuleMetadata> findByIdAndLastUpdate(UUID id, Date lastUpdate)

  @Query("Select * from GranuleMetadata where granule_id =?0")
  Iterable<GranuleMetadata> findByMetadataId(UUID id)

  @Query("Select * from GranuleMetadata where tracking_id =?0 ALLOW FILTERING")
  Iterable<GranuleMetadata> findByTrackingId(String id)

  @Query("Select * from GranuleMetadata where granule_id =?0 AND last_update>=?1")
  Iterable<GranuleMetadata> findByMetadataIdFromDate(UUID id, Date date)

  @Query("SELECT*FROM GranuleMetadata WHERE dataset=?0")
  Iterable<GranuleMetadata> findByDataset(String dataset)

  @Query("SELECT*FROM GranuleMetadata WHERE dataset=?0 AND last_update>=?1")
  Iterable<GranuleMetadata> findByDatasetFrom(String dataset, Date date)

  @Query("SELECT*FROM GranuleMetadata WHERE granule_schema=?0")
  Iterable<GranuleMetadata> findBySchema(String schema)

  //this does not work, cant search two indexed fields
  @Query("SELECT*FROM GranuleMetadata WHERE dataset =?0 AND granule_schema=?1")
  Iterable<GranuleMetadata> findByDatasetAndSchema(String dataset, String schema)

  @Query("SELECT DISTINCT granule_id FROM GranuleMetadata WHERE dataset=?0")
  Iterable<GranuleMetadata> findDistinctTrackingIdsByDataset(String dataset)

  @Query("SELECT granule_id FROM GranuleMetadata WHERE granule_id=?0 LIMIT 1")
  Iterable<GranuleMetadata> findLatestByMetadataId(UUID granule_id)

  @Query("DELETE FROM GranuleMetadata WHERE granule_id =?0 AND last_update=?1")
  Iterable<GranuleMetadata> deleteByMetadataIdAndLastUpdate(UUID id, Date lastUpdate)

}


