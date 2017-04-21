package ncei.catalog.domain

import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query

interface GranuleMetadataRepository extends CassandraRepository<GranuleMetadata> {

  @Query("Select * from GranuleMetadata where metadata_id =?0 and last_update=?1")
  Iterable<GranuleMetadata> findByIdAndLastUpdate(UUID id, Date lastUpdate)

  @Query("Select last_update from GranuleMetadata where metadata_id =?0")
  Iterable<GranuleMetadata> findTimestampByMetadataId(UUID id)

//    @Query("SELECT*FROM GranuleMetadata WHERE filename=?0 LIMIT ?1")
//    Iterable<GranuleMetadata> findByFilename(String filename, Integer limit)
//
//    @Query("SELECT*FROM GranuleMetadata WHERE filename=?0 AND tracking_id<?1 LIMIT ?2")
//    Iterable<GranuleMetadata> findByFilenameFrom(String filename, String from, Integer limit)
//
//    @Query("SELECT*FROM GranuleMetadata WHERE dataset=?0 ALLOW FILTERING")
//    Iterable<GranuleMetadata> findByDataset(String dataset)

}
