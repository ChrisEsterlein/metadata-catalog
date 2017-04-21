CREATE KEYSPACE IF NOT EXISTS metacat WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}  AND durable_writes = true;
USE metacat;
DROP TABLE IF EXISTS GranuleMetadata;
CREATE TABLE IF NOT EXISTS GranuleMetadata (
    metadata_id timeuuid,
    last_update timestamp,
    tracking_id text,
    filename text,
    dataset text,
    type text,
    granule_size int,
    granule_metadata text,
    geometry text,
    granule_schema text,
    collection list<text>,
    PRIMARY KEY ((metadata_id), last_update)
) with CLUSTERING order by (last_update desc);

CREATE INDEX IF NOT EXISTS DatasetIndex ON GranuleMetadata (dataset);
CREATE INDEX IF NOT EXISTS GranuleSchemaIndex ON GranuleMetadata (granule_schema);

DROP TABLE IF EXISTS CollectionMetadata;
CREATE TABLE IF NOT EXISTS MetadataCatalog.CollectionMetadata (
    last_update timestamp,
    collection_name text,
    collection_schema text,
    type text,
    collection_metadata text,
    geometry text,
    PRIMARY KEY (collection_name, last_update)
);

DROP TABLE IF EXISTS ServicesMetadata;
CREATE TABLE IF NOT EXISTS ServicesMetadata (
    last_update timestamp,
    service_name text,
    service_schema text,
    service_metadata text,
    PRIMARY KEY (service_name, last_update)
);

DROP TABLE IF EXISTS MetadataSchemas;
CREATE TABLE IF NOT EXISTS MetadataSchemas (
    last_update timestamp,
    schema_name text,
    json_schema text,
    PRIMARY KEY (schema_name, last_update)
);
