package ncei.catalog.domain

import com.datastax.driver.core.utils.UUIDs
import org.springframework.cassandra.core.Ordering
import org.springframework.cassandra.core.PrimaryKeyType
import org.springframework.data.cassandra.mapping.Indexed
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.mapping.Table

@Table(value='MetadataSchema')
class MetadataSchema {


    @PrimaryKeyColumn(name = "schema_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    UUID schema_id

    @PrimaryKeyColumn(name = "last_update", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    Date last_update

    @Indexed
    String schema_name

    String json_schema

    MetadataSchema() {
        this.schema_id = UUIDs.timeBased()
        this.last_update = new Date()
    }

}
