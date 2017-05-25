package ncei.catalog.domain

import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query

interface CollectionMetadataRepository extends CassandraRepository<CollectionMetadata> {

  @Query("Select * from CollectionMetadata where id =?0 and last_update=?1")
  Iterable<CollectionMetadata> findByIdAndLastUpdate(UUID id, Date lastUpdate)

  @Query("Select * from CollectionMetadata where id =?0")
  Iterable<CollectionMetadata> findByMetadataId(UUID id)

  @Query("SELECT * FROM CollectionMetadata WHERE id =?0 LIMIT 1")
  Iterable<CollectionMetadata> findByMetadataIdLimitOne(UUID id)

  @Query("SELECT*FROM CollectionMetadata WHERE name=?0")
  Iterable<CollectionMetadata> findByCollectionName(String name)

  @Query("SELECT*FROM CollectionMetadata WHERE metadata_schema=?0")
  Iterable<CollectionMetadata> findBySchema(String schema)

  @Query("SELECT DISTINCT id FROM CollectionMetadata WHERE name=?0")
  Iterable<CollectionMetadata> findDistinctTrackingIdsByCollectionName(String name)

  @Query("SELECT id FROM CollectionMetadata WHERE id=?0 LIMIT 1")
  Iterable<CollectionMetadata> findLatestByMetadataId(UUID id)

  @Query("DELETE FROM CollectionMetadata WHERE id =?0 AND last_update=?1")
  Iterable<CollectionMetadata> deleteByMetadataIdAndLastUpdate(UUID id, Date lastUpdate)

}