#!/bin/bash

replicas=1

while getopts ":r:" opt; do
  case $opt in
    r) replicas="$OPTARG"
    shift; shift
    ;;
    \?) echo "Invalid option -$OPTARG"
    ;;
  esac
done

printf "Number of requested replicas is %s\n" "$replicas"


# See http://stackoverflow.com/questions/8818119/linux-how-can-i-run-a-function-from-a-script-in-command-line
# ./start-services.sh "-local"

NATS_USERNAME="smartmeter"
NATS_PASSWORD="xyz1234"

create_network() {
docker network create --driver overlay smart-meter-net
#docker service rm $(docker service ls -q)
}

create_service_cassandra() {
# https://hub.docker.com/_/cassandra/
# http://serverfault.com/questions/806649/docker-swarm-and-volumes
# https://clusterhq.com/2016/03/09/fun-with-swarm-part1/
docker service create \
	--name cassandra-root \
	--replicas=${replicas} \
	--network smart-meter-net \
	--mount type=volume,source=cassandra-volume,destination=/var/lib/cassandra \
	-e CASSANDRA_BROADCAST_ADDRESS="cassandra-root" \
	-e CASSANDRA_CLUSTER_NAME="Smartmeter Cluster" \
	-p 9042:9042 \
	-p 9160:9160 \
	logimethods/smart-meter:cassandra$1
}

create_service_spark() {
docker service create \
	--name spark-master \
	-e SERVICE_NAME=spark-master \
	--network smart-meter-net \
	--constraint 'node.role == manager' \
	--log-driver=json-file \
	gettyimages/spark:2.0.2-hadoop-2.7

docker service create \
	--name spark-slave \
	-e SERVICE_NAME=spark-slave \
	--network smart-meter-net \
	--replicas=${replicas} \
	gettyimages/spark:2.0.2-hadoop-2.7 \
		bin/spark-class org.apache.spark.deploy.worker.Worker spark://spark-master:7077
}

create_service_nats() {
docker service create \
	--name nats \
	--network smart-meter-net \
	--replicas=${replicas} \
	-e NATS_USERNAME=${NATS_USERNAME} \
	-e NATS_PASSWORD=${NATS_PASSWORD} \
	logimethods/smart-meter:nats-server$1
}

create_service_app-streaming() {
#docker pull logimethods/smart-meter:app-streaming
docker service create \
	--name app-streaming \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e SPARK_MASTER_URL=spark://spark-master:7077 \
	-e LOG_LEVEL=INFO \
	--network smart-meter-net \
	--replicas=${replicas} \
	logimethods/smart-meter:app-streaming$1 \
		"smartmeter.voltage.data.>" "smartmeter.voltage.data. => smartmeter.voltage.extract.max."
}

create_service_monitor() {
#docker pull logimethods/smart-meter:monitor
docker service create \
	--name monitor \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	--network smart-meter-net \
	--replicas=${replicas} \
	logimethods/smart-meter:monitor$1 \
		"smartmeter.voltage.extract.>"
}

create_service_cassandra() {
#docker pull logimethods/nats-reporter
docker service create \
	--name reporter \
	--network smart-meter-net \
	--replicas=${replicas} \
	-p 8888:8080 \
	logimethods/nats-reporter
}

create_cassandra_cassandra-inject() {
# Create the Cassandra Tables
echo "Will create the Cassandra Messages Table"
until docker exec -it $(docker ps | grep "cassandra-root" | rev | cut -d' ' -f1 | rev) cqlsh -f '/cql/create-timeseries.cql'; do echo "Try again to create the Cassandra Time Series Table"; sleep 4; done
}

create_service_cassandra() {
docker service create \
	--name cassandra-inject \
	--network smart-meter-net \
	--replicas=${replicas} \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e NATS_SUBJECT="smartmeter.voltage.data.>" \
	-e CASSANDRA_URL=$(docker ps | grep "cassandra-root" | rev | cut -d' ' -f1 | rev) \
	logimethods/smart-meter:cassandra-inject$1
}

create_service_inject() {
#docker pull logimethods/smart-meter:inject
docker service create \
	--name inject \
	-e GATLING_TO_NATS_SUBJECT=smartmeter.voltage.data \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	--network smart-meter-net \
	--replicas=${replicas} \
	logimethods/smart-meter:inject$1 \
		--no-reports -s com.logimethods.smartmeter.inject.NatsInjection
}
		
"$@"
