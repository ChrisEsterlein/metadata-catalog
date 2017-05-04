# INDEX
This is the Elasticsearch API for searching metadata.  It listens to a rabbit queue for work to do.

## Prerequisites:
1. Install docker

The following is required if not using the docker containers to run Elasticsearch and the app:
1. Install elasticsearch 2.4 (NOTE: version 5 isn't supported by spring data 1.5.2) - 'brew install elasticsearch'
1. To start Elasticsearch - type in command line 'elasticsearch' or do -d to run as daemon which requires killing the PID when done.
1. Create an application.yml at index module root using SAMPLE-application.yml

## Developer setup
1. Run ./gradlew index:bootrun to start this module
1. Run ./gradlew index:dockerBootrun to start this module in containers

##Saving via RABBITMQ
Save metadata:
Properties: content_type = application/json
Payload: {"GRANULE_ID":"1asdf123s", "dataset":"csb", "fileName":"/blah/fileName1"}

##Searching via REST
http://localhost:8088/index/search?q=dataset:csb fileName:/blah/fileName1
The parameter is a simple query string as a parameter as a URI search as defined by Elastcisearch.

##Elasticsearch commands
URI Search - simple query string as a parameter
http://localhost:9200/search_index/_search?q=dataset:csb