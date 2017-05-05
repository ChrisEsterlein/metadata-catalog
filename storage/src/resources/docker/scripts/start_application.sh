#!/bin/bash

# function to check if Cassandra has completed initialization
check() {
  # see if port 9042 is available using bash TCP redirection
  timeout 1 bash -c 'cat < /dev/null > /dev/tcp/storageCassandra/9042'
  val=$?
  echo "Cassandra connection status (0=healthy): $val" 1>&2;
  if [ $val ]
  then
    # see if the initialization container has finished
    # TODO I'm expecting (0=incomplete) for initialization, so this might be backwards - it's working so long as initialization itself is very fast, otherwise it might present a timing issue
    timeout 1 bash -c 'cat < /dev/null > /dev/tcp/wait/80'
    val2=$?
    echo "Cassandra initialization status: $val2" 1>&2;
    return $val && $val2
  fi
  return $val
}

# wait until status is complete
while ! $(check);
  do
    sleep 1;
  done;

# start the application by running the jar
java -jar /usr/src/app/storage-0.1.0.jar;