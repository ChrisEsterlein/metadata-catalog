#!/bin/bash

# function to check if ElasticSearch is available
check() {
  # see if port 9200 is available using bash TCP redirection
  timeout 1 bash -c 'cat < /dev/null > /dev/tcp/es/9200'
  val=$?
  echo "Elasticsearch connection status (0=healthy): $val" 1>&2;
  return $val
}

# wait until status is complete
while ! $(check);
  do
    sleep 1;
  done;

# start the application by running the jar
java -jar index-0.1.0-SNAPSHOT.jar;