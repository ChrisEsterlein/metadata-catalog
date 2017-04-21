package ncei.catalog.domain

import org.springframework.cassandra.core.Ordering
import org.springframework.cassandra.core.PrimaryKeyType
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.mapping.Table

@Table(value='CollectionMetadata')
class CollectionMetadata {

    @PrimaryKeyColumn(name = "collection_name", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    String collection_name

    @PrimaryKeyColumn(name = "last_update", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    Date last_update

    String collection_schema
    String type
    String collection_metadata

}
