docker service create \
	--name cassandra-client \
	--mode=global \
	--network smart-meter-net \
	cassandra:3.0.9
	sleep infinity
	
sleep 5

docker exec -it $(docker ps | grep "cassandra-client" | rev | cut -d' ' -f1 | rev) cqlsh $(docker ps | grep "cassandra-root" | rev | cut -d' ' -f1 | rev)
