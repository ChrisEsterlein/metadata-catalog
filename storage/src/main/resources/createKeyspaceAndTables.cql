CREATE KEYSPACE IF NOT exists metacat WITH replication = {'class':'SimpleStrategy', 'replication_factor':'1'}  AND durable_writes = true;

USE metacat;


CREATE TABLE IF NOT exists GranuleMetadata (
id uuid,
last_update timestamp,
filename text,
dataset text,
type text,
access_protocol text,
file_path text,
size_bytes int,
metadata text,
geometry text,
metadata_schema text,
collections list < text >,
deleted boolean,
PRIMARY KEY ((id), last_update)
)
WITH CLUSTERING
  ORDER BY (last_update DESC);

CREATE INDEX IF NOT exists DatasetIndex ON GranuleMetadata (dataset);
CREATE INDEX IF NOT exists GranuleSchemaIndex ON GranuleMetadata (metadata_schema);

CREATE TABLE IF NOT exists CollectionMetadata (
id timeuuid,
last_update timestamp,
name text,
metadata_schema text,
type text,
metadata text,
geometry text,
deleted boolean,
submission_id text,
PRIMARY KEY ((id), last_update)
) WITH CLUSTERING ORDER BY (last_update DESC);

CREATE INDEX IF NOT exists CollectionIndex ON CollectionMetadata (name);
CREATE INDEX IF NOT exists CollectionSchemaIndex ON CollectionMetadata (metadata_schema);

CREATE TABLE IF NOT exists ServicesMetadata (
last_update timestamp,
service_name text,
service_schema text,
service_metadata text,
deleted boolean,
PRIMARY KEY (service_name, last_update))
WITH CLUSTERING ORDER BY (last_update DESC);

DROP TABLE IF exists MetadataSchema;
CREATE TABLE IF NOT exists MetadataSchema (
id timeuuid,
last_update timestamp,
name text,
json_schema text,
deleted boolean,
PRIMARY KEY (id, last_update)
) WITH CLUSTERING ORDER BY (last_update DESC);

CREATE INDEX IF NOT exists SchemaNameIndex ON MetadataSchema (name);
