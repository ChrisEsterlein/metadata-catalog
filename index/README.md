# INDEX
This is the Elasticsearch API for searching metadata.

## Prerequisites:
1. Install elasticsearch 2.4 (NOTE: version 5 isn't supported by spring data 1.5.2) - 'brew install elasticsearch'
1. To start Elasticsearch - type in command line 'elasticsearch' or do -d to run as daemon which requires killing the PID when done.
1. Create an application.yml at index module root using SAMPLE-application.yml

## Developer setup
1. Run ./gradlew index:bootrun to start this module

##API
Save - Do a put to:
http://localhost:8088/index/files?
Example body (every field may not be saved in elasticsearch if not specified in the code to do so):
{"fileName":"name2","dataset":"csb","type":"data","file_size":"1","file_metadata":"filepath","geometry":"null"}

##Elasticsearch commands
Search - in the index 'search_index' for parameter dataset of 'csb':
http://localhost:9200/search_index/_search?q=dataset:csb