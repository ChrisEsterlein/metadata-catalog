#!/bin/bash

# function to check if Cassandra is available
check() {
  # see if port ${cassandra_port} is available using bash TCP redirection
  timeout 1 bash -c 'cat < /dev/null > /dev/tcp/${cassandra_service_name}/${cassandra_port}'
  val=\$?
  echo "Cassandra connection status (0=healthy): \$val" 1>&2;
  return \$val
}

# wait until status is complete
while ! \$(check);
  do
    sleep 1;
  done;

# start the application by running the jar
java -jar ${storage_jar};