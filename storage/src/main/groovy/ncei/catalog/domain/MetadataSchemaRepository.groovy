package ncei.catalog.domain

import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query

interface MetadataSchemaRepository extends CassandraRepository<MetadataSchema> {

  @Query("Select * from MetadataSchema where schema_id =?0 and last_update=?1")
  Iterable<MetadataSchema> findByIdAndLastUpdate(UUID id, Date lastUpdate)

  @Query("Select * from MetadataSchema where schema_id =?0")
  Iterable<MetadataSchema> findByMetadataId(UUID id)

  @Query("SELECT*FROM MetadataSchema WHERE schema_name=?0")
  Iterable<MetadataSchema> findBySchemaName(String schema_name)

  @Query("SELECT*FROM MetadataSchema WHERE granule_schema=?0")
  Iterable<MetadataSchema> findBySchema(String schema)

  @Query("SELECT*FROM MetadataSchema WHERE schema_name =?0 AND granule_schema=?1")
  Iterable<MetadataSchema> findBySchemaNameAndSchema(String schema_name, String schema)

  @Query("SELECT DISTINCT schema_id FROM MetadataSchema WHERE schema_name=?0")
  Iterable<MetadataSchema> findDistinctTrackingIdsBySchemaName(String schema_name)


  @Query("SELECT schema_id FROM MetadataSchema WHERE schema_id=?0 LIMIT 1")
  Iterable<MetadataSchema> findLatestByMetadataId(UUID schema_id)

  @Query("DELETE FROM MetadataSchema WHERE schema_id =?0 AND last_update=?1")
  Iterable<MetadataSchema> deleteByMetadataIdAndLastUpdate(UUID id, Date lastUpdate)

}