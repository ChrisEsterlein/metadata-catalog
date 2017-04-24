package ncei.catalog.service

import ncei.catalog.model.Metadata
import ncei.catalog.repository.MetadataRepository
import org.springframework.beans.factory.annotation.Autowired

@org.springframework.stereotype.Service
class Service {
  @Autowired
  MetadataRepository repository

  Metadata save(Metadata metadata) {
    repository.save(metadata)
  }
}
