#!/bin/bash

# function to test if Cassandra is available to initialize
check() {
  # see if port 9042 is available using bash TCP redirection
  timeout 1 bash -c 'cat < /dev/null > /dev/tcp/storageCassandra/9042'
  val=$?
  echo "Cassandra connection status (0=healthy): $val" 1>&2;
  return $val
}

# wait until the status changes
while ! $(check);
  do
    sleep 1;
  done;

# Once the connection is available, use the script to initialize the database
cqlsh storageCassandra 9042 <<< $(cat /tmp/cql/createKeyspaceAndTables.cql)