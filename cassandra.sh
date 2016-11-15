# https://github.com/docker/docker/issues/27157
docker network create -d overlay smart-meter-net

# https://hub.docker.com/_/cassandra/
# http://serverfault.com/questions/806649/docker-swarm-and-volumes
# https://clusterhq.com/2016/03/09/fun-with-swarm-part1/
docker service create \
	--name cassandra-root \
	--replicas 1 \
	--network smart-meter-net \
	-e CASSANDRA_BROADCAST_ADDRESS="cassandra-root" \
	cassandra:3.0.9

sleep 5

docker service create \
	--name cassandra-cluster \
	--replicas 1 \
	-e CASSANDRA_BROADCAST_ADDRESS="cassandra-cluster" \
	-e CASSANDRA_SEEDS="cassandra-root" \
	--network smart-meter-net \
	cassandra:3.0.9

docker service create \
	--name cassandra-client \
	--mode=global \
	--network smart-meter-net \
	cassandra:3.0.9
	sleep infinity
