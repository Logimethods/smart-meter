#!/bin/sh

#renice 19 -p $$

# /nodetool/dsc-cassandra-3.0.9/bin/nodetool -h cassandra_main -u cassandra -pw ${JMX_PASSWORD} cfstats smartmeter.raw_voltage_data  | grep "Local write count" | rev | cut -d ' ' -f 1 | rev
if [ ! -f "tmp_value.lock" ]
then
  echo $(date) >> tmp_value.lock

  values=$( /nodetool/dsc-cassandra-3.0.9/bin/nodetool -h ${CASSANDRA_URL} -u cassandra -pw ${JMX_PASSWORD} cfstats ${TELEGRAF_CASSANDRA_TABLE}  | grep "Local write count" | rev | cut -d ' ' -f 1 | rev )

  total=0
  IFS=$'\n'
  for value in $values
  do
    total=$(( total + value ))
  done

  echo "$total" > tmp_value.txt
  rm tmp_value.lock
fi
