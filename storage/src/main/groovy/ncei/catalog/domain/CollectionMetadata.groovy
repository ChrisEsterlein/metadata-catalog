package ncei.catalog.domain

import com.datastax.driver.core.utils.UUIDs
import org.springframework.cassandra.core.Ordering
import org.springframework.cassandra.core.PrimaryKeyType
import org.springframework.data.cassandra.mapping.Indexed
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.mapping.Table

@Table(value='CollectionMetadata')
class CollectionMetadata {

    @PrimaryKeyColumn(name = "collection_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    UUID collection_id

    @PrimaryKeyColumn(name = "last_update", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    Date last_update

    @Indexed
    String collection_name

    @Indexed
    String collection_schema

    String type
    String collection_metadata

    CollectionMetadata() {
        this.collection_id = UUIDs.timeBased()
        this.last_update = new Date()
    }
}
