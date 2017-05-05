#!/bin/bash


check() {
  timeout 1 bash -c 'cat < /dev/null > /dev/tcp/storageCassandra/9042'
  val=$?
  echo "status storage $val" 1>&2;
  if [ $val ]
  then
    timeout 1 bash -c 'cat < /dev/null > /dev/tcp/wait/80'
    val2=$?
    echo "status init $val2" 1>&2;
    return $val && $val2
  fi
  return $val
}

while ! $(check);
  do
    echo sleeping;
    sleep 1;
  done;
echo Connected;
java -jar /usr/src/app/storage-0.1.0.jar;