docker service create \
	--name cassandra-cluster \
	--replicas 1 \
	-e CASSANDRA_BROADCAST_ADDRESS="cassandra-cluster" \
	-e CASSANDRA_SEEDS="cassandra-root" \
	--network smart-meter-net \
	cassandra:3.0.9
