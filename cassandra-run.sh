docker service create \
	--name cassandra-root \
	--replicas 1 \
	--network smart-meter-net \
	--mount type=volume,source=cassandra-volume,destination=/var/lib/cassandra \
	-e CASSANDRA_BROADCAST_ADDRESS="cassandra-root" \
	-e CASSANDRA_CLUSTER_NAME="Smartmeter Cluster" \
	-p 9042:9042 \
	-p 9160:9160 \
	logimethods/smart-meter:cassandra$1

