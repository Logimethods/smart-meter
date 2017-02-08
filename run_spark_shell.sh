#!/bin/bash

set -a # turn on auto-export
. configuration.properties
set -a # turn off auto-export

docker run --network smart-meter-net -it logimethods/smart-meter:app-batch spark-shell --master spark://spark-master:7077 --conf spark.cassandra.connection.host=$(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) --packages datastax:spark-cassandra-connector:${spark_cassandra_connector_version}
