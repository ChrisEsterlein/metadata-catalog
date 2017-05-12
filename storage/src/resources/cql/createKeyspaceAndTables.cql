CREATE KEYSPACE IF NOT exists metacat
WITH replication = {'class':'SimpleStrategy', 'replication_factor':'1'}  AND durable_writes = true

;
USE metacat

;
DROP TABLE IF exists GranuleMetadata;
CREATE TABLE IF NOT exists GranuleMetadata (
id timeuuid,
last_update timestamp,
tracking_id text,
filename text,
dataset text,
type text,
access_protocol text,
granule_size int,
granule_metadata text,
geometry text,
granule_schema text,
collections list < text >,
deleted boolean,
PRIMARY KEY ((id), last_update)
)
WITH CLUSTERING
  ORDER BY (last_update DESC)

;

CREATE INDEX IF NOT exists DatasetIndex ON GranuleMetadata (dataset);
CREATE INDEX IF NOT exists GranuleSchemaIndex ON GranuleMetadata (granule_schema);

DROP TABLE IF exists CollectionMetadata;
CREATE TABLE IF NOT exists CollectionMetadata (
id timeuuid,
last_update timestamp,
collection_name text,
collection_schema text,
type text,
collection_metadata text,
geometry text,
deleted boolean,
submission_id text,
PRIMARY KEY ((id), last_update)
)
WITH CLUSTERING
  ORDER BY (last_update DESC)

;

CREATE INDEX IF NOT exists CollectionIndex ON CollectionMetadata (collection_name);
CREATE INDEX IF NOT exists CollectionSchemaIndex ON CollectionMetadata (collection_schema);

DROP TABLE IF exists ServicesMetadata;
CREATE TABLE IF NOT exists ServicesMetadata (
last_update timestamp,
service_name text,
service_schema text,
service_metadata text,
deleted boolean,
PRIMARY KEY (service_name, last_update)
)
WITH CLUSTERING
  ORDER BY (last_update DESC)

;

DROP TABLE IF exists MetadataSchema;
CREATE TABLE IF NOT exists MetadataSchema (
id timeuuid,
last_update timestamp,
schema_name text,
json_schema text,
deleted boolean,
PRIMARY KEY (id, last_update)
)
WITH CLUSTERING
  ORDER BY (last_update DESC)

;

CREATE INDEX IF NOT exists SchemaNameIndex ON MetadataSchema (schema_name);

