package ncei.catalog.model

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document

@Document(indexName = 'search_index', type = 'metadata')
class Metadata {

    @Id
    String id

    String dataset

    String fileName

    String toString() {
        "id: $id, dataset: $dataset, fileName: $fileName"
    }
}
