source ./services_hierarchy-main.sh

inject="inject inject-${SECRET_MODE} inject-${CLUSTER_MODE} $root"
inject_metrics="$inject inject_metrics $metrics"

cassandra_inject="cassandra_inject cassandra_inject-${CLUSTER_MODE} cassandra_inject-${SECRET_MODE} $cassandra $root"
cassandra_inject_metrics="cassandra_inject_metrics $cassandra $metrics"

streaming="streaming streaming-${SECRET_MODE} $root $spark"
streaming_metrics="streaming_metrics streaming_metrics-${SECRET_MODE} $streaming $metrics"

monitoring="monitoring monitoring-${SECRET_MODE} $root"

test="base test test-${SECRET_MODE}"

integration_app="$inject $streaming $monitoring"
integration_app_monitoring="$inject_monitoring $streaming_monitoring $monitoring_monitoring"

prediction="prediction prediction${SECRET_MODE} $root $spark $cassandra $hadoop"
prediction_metrics="prediction_metrics $prediction $metrics"

