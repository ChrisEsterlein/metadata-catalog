#!/bin/bash


check() {
  timeout 1 bash -c 'cat < /dev/null > /dev/tcp/storageCassandra/9042'
  val=$?
  echo "status $val" 1>&2;
  return $val
}

while ! $(check);
  do
    echo sleeping;
    sleep 1;
  done;
echo Connected;
cqlsh storageCassandra 9042 <<< $(cat /tmp/cql/createKeyspaceAndTables.cql)