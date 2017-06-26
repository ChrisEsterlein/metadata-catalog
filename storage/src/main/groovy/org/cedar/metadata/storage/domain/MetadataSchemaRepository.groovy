package org.cedar.metadata.storage.domain

import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query

interface MetadataSchemaRepository extends CassandraRepository<MetadataSchema> {

  @Query("Select * from MetadataSchema where id =?0 and last_update=?1")
  Iterable<MetadataSchema> findByIdAndLastUpdate(UUID id, Date lastUpdate)

  @Query("Select * from MetadataSchema where id =?0")
  Iterable<MetadataSchema> findByMetadataId(UUID id)

  @Query("Select * from MetadataSchema where id =?0 LIMIT 1")
  Iterable<MetadataSchema> findByMetadataIdLimitOne(UUID id)

  @Query("SELECT*FROM MetadataSchema WHERE name=?0")
  Iterable<MetadataSchema> findBySchemaName(String name)

  @Query("SELECT id FROM MetadataSchema WHERE id=?0 LIMIT 1")
  Iterable<MetadataSchema> findLatestByMetadataId(UUID id)

  @Query("DELETE FROM MetadataSchema WHERE id =?0 AND last_update=?1")
  Iterable<MetadataSchema> deleteByMetadataIdAndLastUpdate(UUID id, Date lastUpdate)

}
