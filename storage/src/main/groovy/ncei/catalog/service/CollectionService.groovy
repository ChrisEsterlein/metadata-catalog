package ncei.catalog.service

import ncei.catalog.domain.CollectionMetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CollectionService {

  @Autowired
  CollectionMetadataRepository collectionMetadataRepository


}
