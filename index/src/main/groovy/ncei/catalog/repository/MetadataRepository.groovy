package ncei.catalog.repository

import ncei.catalog.model.Metadata
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

@Repository
interface MetadataRepository extends ElasticsearchRepository<Metadata, String> {

//    Page<Metadata> findByDataset(String dataset, Pageable pageable)
}
