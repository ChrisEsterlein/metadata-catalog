package ncei.catalog.domain

import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query

interface GranuleMetadataRepository extends CassandraRepository<GranuleMetadata> {

  @Query("Select * from GranuleMetadata LIMIT ?0")
  Iterable<GranuleMetadata> findAllWithLimit(int limit)

  @Query("Select * from GranuleMetadata where id=?0 and last_update=?1")
  Iterable<GranuleMetadata> findByIdAndLastUpdate(UUID id, Date lastUpdate)

  @Query("Select * from GranuleMetadata where id=?0 LIMIT 1")
  Iterable<GranuleMetadata> findByMetadataIdLimitOne(UUID id)

  @Query("Select * from GranuleMetadata where id=?0")
  Iterable<GranuleMetadata> findByMetadataId(UUID id)

  @Query("DELETE FROM GranuleMetadata WHERE id=?0 AND last_update=?1")
  Iterable<GranuleMetadata> deleteByMetadataIdAndLastUpdate(UUID id, Date lastUpdate)

}


