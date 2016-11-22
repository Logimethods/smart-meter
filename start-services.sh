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
	-e CASSANDRA_BROADCAST_ADDRESS="cassandra-root" \
	-e CASSANDRA_CLUSTER_NAME="Smartmeter Cluster" \
	logimethods/smart-meter:cassandra$1

docker service create \
	--name spark-master \
	-e SERVICE_NAME=spark-master \
	--network smart-meter-net \
	--constraint 'node.role == manager' \
	--log-driver=json-file \
	gettyimages/spark:2.0.1-hadoop-2.7
	
docker service create \
	--name spark-slave \
	-e SERVICE_NAME=spark-slave \
	--network smart-meter-net \
	--replicas=2 \
	gettyimages/spark:2.0.1-hadoop-2.7 \
		bin/spark-class org.apache.spark.deploy.worker.Worker spark://spark-master:7077
		
docker service create \
	--name nats \
	--network smart-meter-net \
	--replicas=1 \
	-e NATS_USERNAME=${NATS_USERNAME} \
	-e NATS_PASSWORD=${NATS_PASSWORD} \
	nats

#docker pull logimethods/smart-meter:app-streaming
docker service create \
	--name app-streaming \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e SPARK_MASTER_URL=spark://spark-master:7077 \
	-e LOG_LEVEL=INFO \
	--network smart-meter-net \
	--replicas=1 \
	logimethods/smart-meter:app-streaming$1 \
		"smartmeter.voltage.data.>" "smartmeter.voltage.data. => smartmeter.voltage.extract.max."

#docker pull logimethods/smart-meter:monitor
docker service create \
	--name monitor \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	--network smart-meter-net \
	--replicas=1 \
	logimethods/smart-meter:monitor$1 \
		"smartmeter.voltage.extract.>"

#docker pull logimethods/nats-reporter
docker service create \
	--name reporter \
	--network smart-meter-net \
	--replicas=1 \
	-p 8888:8080 \
	logimethods/nats-reporter

# Create the Cassandra Tables
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
