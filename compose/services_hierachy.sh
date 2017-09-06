root="root root${SECRET_MODE}"

metrics="metrics $root"
spark="spark $root"
cassandra="cassandra $root"
hadoop="hadoop $root"

root_metrics="root_metrics $root $metrics"

inject="inject inject${SECRET_MODE} $root $cassandra"
inject_metrics="inject_metrics $inject $metrics"

streaming="streaming streaming${SECRET_MODE} $root $spark"
streaming_metrics="streaming_metrics streaming_metrics${SECRET_MODE} $streaming $metrics"

prediction="prediction prediction${SECRET_MODE} $root $spark $cassandra $hadoop"
prediction_metrics="prediction_metrics $prediction $metrics"
