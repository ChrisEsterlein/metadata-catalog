package ncei.catalog.domain

import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query

interface MetadataSchemaRepository extends CassandraRepository<MetadataSchema> {

  @Query("Select * from MetadataSchema where id =?0 and last_update=?1")
  Iterable<MetadataSchema> findByIdAndLastUpdate(UUID id, Date lastUpdate)

  @Query("Select * from MetadataSchema where id =?0")
  Iterable<MetadataSchema> findByMetadataId(UUID id)

  @Query("Select * from MetadataSchema where id =?0 LIMIT 1")
  Iterable<MetadataSchema> findByMetadataIdLimitOne(UUID id)

  @Query("SELECT*FROM MetadataSchema WHERE metadata_schema=?0")
  Iterable<MetadataSchema> findBySchemaName(String metadata_schema)

  @Query("SELECT*FROM MetadataSchema WHERE granule_schema=?0")
  Iterable<MetadataSchema> findBySchema(String schema)

  @Query("SELECT*FROM MetadataSchema WHERE metadata_schema =?0 AND granule_schema=?1")
  Iterable<MetadataSchema> findBySchemaNameAndSchema(String metadata_schema, String schema)

  @Query("SELECT DISTINCT id FROM MetadataSchema WHERE metadata_schema=?0")
  Iterable<MetadataSchema> findDistinctTrackingIdsBySchemaName(String metadata_schema)


  @Query("SELECT id FROM MetadataSchema WHERE id=?0 LIMIT 1")
  Iterable<MetadataSchema> findLatestByMetadataId(UUID id)

  @Query("DELETE FROM MetadataSchema WHERE id =?0 AND last_update=?1")
  Iterable<MetadataSchema> deleteByMetadataIdAndLastUpdate(UUID id, Date lastUpdate)

}