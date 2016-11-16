docker service create \
	--name cassandra-inject \
	--network smart-meter-net \
	-e CASSANDRA_CLUSTER=$(docker ps | grep "cassandra-root" | rev | cut -d' ' -f1 | rev) \
	cassandra-inject
