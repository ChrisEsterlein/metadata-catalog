package ncei.catalog.domain

import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query

interface CollectionMetadataRepository extends CassandraRepository<CollectionMetadata> {

  @Query("Select * from CollectionMetadata where collection_id =?0 and last_update=?1")
  Iterable<CollectionMetadata> findByIdAndLastUpdate(UUID id, Date lastUpdate)

  @Query("Select * from CollectionMetadata where collection_id =?0")
  Iterable<CollectionMetadata> findByMetadataId(UUID id)

//    @Query("SELECT*FROM CollectionMetadata WHERE filename=?0 LIMIT ?1")
//    Iterable<CollectionMetadata> findByFilename(String filename, Integer limit)
//
//    @Query("SELECT*FROM CollectionMetadata WHERE filename=?0 AND tracking_id<?1 LIMIT ?2")
//    Iterable<CollectionMetadata> findByFilenameFrom(String filename, String from, Integer limit)
//
  @Query("SELECT*FROM CollectionMetadata WHERE collection_name=?0")
  Iterable<CollectionMetadata> findByCollectionName(String collection_name)

  @Query("SELECT*FROM CollectionMetadata WHERE granule_schema=?0")
  Iterable<CollectionMetadata> findBySchema(String schema)

  @Query("SELECT*FROM CollectionMetadata WHERE collection_name =?0 AND granule_schema=?1")
  Iterable<CollectionMetadata> findByCollectionNameAndSchema(String collection_name, String schema)

  //this is not working
  @Query("SELECT DISTINCT collection_id FROM CollectionMetadata WHERE collection_name=?0")
  Iterable<CollectionMetadata> findDistinctTrackingIdsByCollectionName(String collection_name)


  @Query("SELECT collection_id FROM CollectionMetadata WHERE collection_id=?0 LIMIT 1")
  Iterable<CollectionMetadata> findLatestByMetadataId(UUID collection_id)

  @Query("DELETE FROM CollectionMetadata WHERE collection_id =?0 AND last_update=?1")
  Iterable<CollectionMetadata> deleteByMetadataId(UUID id, Date lastUpdate)

}