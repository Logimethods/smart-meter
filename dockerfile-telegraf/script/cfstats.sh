#!/bin/sh

/nodetool/dsc-cassandra-3.0.9/bin/nodetool -h cassandra_main -u cassandra -pw XXX23zzz cfstats smartmeter.raw_voltage_data  | grep "Local write count" | rev | cut -d ' ' -f 1 | rev
