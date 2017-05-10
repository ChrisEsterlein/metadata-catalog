package ncei.catalog.service

import groovy.util.logging.Slf4j
import ncei.catalog.domain.CollectionMetadata
import ncei.catalog.domain.CollectionMetadataRepository
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.GranuleMetadataRepository
import ncei.catalog.domain.MetadataRecord
import ncei.catalog.domain.MetadataSchema
import ncei.catalog.domain.MetadataSchemaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.stereotype.Component

@Slf4j
@Component
class ControllerService {

  @Autowired
  GranuleMetadataRepository granuleMetadataRepository
  @Autowired
  CollectionMetadataRepository collectionMetadataRepository
  @Autowired
  MetadataSchemaRepository metadataSchemaRepository

  MetadataRecord toMetadataRecord(String table, Map object) {

    switch (table) {
      case 'collections':
        return new CollectionMetadata(object)
      case 'granules':
        return new GranuleMetadata(object)
      case 'schemas':
        return new MetadataSchema(object)
    }
  }

  CassandraRepository getRepo(String table) {
    switch (table) {
      case 'collections':
        return collectionMetadataRepository
      case 'granules':
        return granuleMetadataRepository
      case 'schemas':
        return metadataSchemaRepository
    }
  }

}
