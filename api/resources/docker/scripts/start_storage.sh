#!/bin/bash

# function to check if Cassandra is available
check() {
  # see if port 9042 is available using bash TCP redirection
  timeout 1 bash -c 'cat < /dev/null > /dev/tcp/cassandra/9042'
  val=$?
  echo "Cassandra connection status (0=healthy): $val" 1>&2;
  return $val
}

# wait until status is complete
while ! $(check);
  do
    sleep 1;
  done;

# start the application by running the jar
java -jar storage-0.1.0-SNAPSHOT.jar;