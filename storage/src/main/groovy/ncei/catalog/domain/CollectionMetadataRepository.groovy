package ncei.catalog.domain

import org.springframework.data.cassandra.repository.CassandraRepository

interface CollectionMetadataRepository extends CassandraRepository<CollectionMetadata> {

}