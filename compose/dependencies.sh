#!/bin/bash

## ./compose/dependencies.sh "_secrets" inject_metrics

SECRET_MODE="$1"
shift 1

root="root"
metrics="metrics $root"
spark="spark $root"
cassandra="cassandra $root"
hadoop="hadoop $root"

inject="inject inject${SECRET_MODE} $root $cassandra"
inject_metrics="inject_metrics $inject $metrics"

streaming="streaming streaming${SECRET_MODE} $root $spark"
streaming_metrics="streaming_metrics $streaming $metrics"

prediction="prediction prediction${SECRET_MODE} $root $spark $cassandra $hadoop"
prediction_metrics="prediction_metrics $prediction $metrics"

yamlreader $( eval echo "\$$@" \
  | xargs -n1 | sort -u | xargs \
  | sed s/["^ "]*/compose'\/'docker-compose-\&.yml/g )
