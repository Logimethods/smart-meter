# ./start-services.sh "-local"

NATS_USERNAME="smartmeter"
NATS_PASSWORD="xyz1234"

docker network create --driver overlay smart-meter-net
#docker service rm $(docker service ls -q)

# https://hub.docker.com/_/cassandra/
# http://serverfault.com/questions/806649/docker-swarm-and-volumes
# https://clusterhq.com/2016/03/09/fun-with-swarm-part1/
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
		
docker service create \
	--name nats \
	--network smart-meter-net \
	--replicas=1 \
	-e NATS_USERNAME=${NATS_USERNAME} \
	-e NATS_PASSWORD=${NATS_PASSWORD} \
	logimethods/smart-meter:nats-server$1

# Create the Cassandra Tables
# https://github.com/docker/docker/blob/master/docs/reference/commandline/service_create.md#add-bind-mounts-or-volumes
echo "Will create the Cassandra Messages Table"
until docker exec -it $(docker ps | grep "cassandra-root" | rev | cut -d' ' -f1 | rev) cqlsh -f '/cql/create-timeseries.cql'; do echo "Try again to create the Cassandra Time Series Table"; sleep 4; done

docker service create \
	--name cassandra-inject \
	--network smart-meter-net \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e NATS_SUBJECT="smartmeter.voltage.data.>" \
	-e CASSANDRA_URL=$(docker ps | grep "cassandra-root" | rev | cut -d' ' -f1 | rev) \
	logimethods/smart-meter:cassandra-inject$1

#docker pull logimethods/smart-meter:inject
docker service create \
	--name inject \
	-e GATLING_TO_NATS_SUBJECT=smartmeter.voltage.data \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	--network smart-meter-net \
	--replicas=1 \
	logimethods/smart-meter:inject$1 \
		--no-reports -s com.logimethods.smartmeter.inject.NatsInjection
