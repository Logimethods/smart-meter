#!/bin/sh

# /nodetool/dsc-cassandra-3.0.9/bin/nodetool -h cassandra_main -u cassandra -pw ${JMX_PASSWORD} cfstats smartmeter.raw_voltage_data  | grep "Local write count" | rev | cut -d ' ' -f 1 | rev

values=$( /nodetool/dsc-cassandra-3.0.9/bin/nodetool -h cassandra_main -u cassandra -pw ${JMX_PASSWORD} cfstats smartmeter  | grep "Local write count" | rev | cut -d ' ' -f 1 | rev )

total=0
IFS=$'\n'
for value in $values
do
  total=$(( total + value ))
done
echo "$total"
