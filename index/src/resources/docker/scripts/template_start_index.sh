#!/bin/bash

# function to check if ElasticSearch is available
check() {
  # see if port ${elasticsearch_rest_port} is available using bash TCP redirection
  timeout 1 bash -c 'cat < /dev/null > /dev/tcp/${elasticsearch_service_name}/${elasticsearch_rest_port}'
  val=\$?
  echo "Elasticsearch connection status (0=healthy): \$val" 1>&2;
  return \$val
}

# wait until status is complete
while ! \$(check);
  do
    sleep 1;
  done;

# start the application by running the jar
java -jar ${index_jar};