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
	--name cassandra \
	--replicas=${replicas} \
	--network smart-meter-net \
	--mount type=volume,source=cassandra-volume,destination=/var/lib/cassandra \
	-e CASSANDRA_BROADCAST_ADDRESS="cassandra" \
	-e CASSANDRA_CLUSTER_NAME="Smartmeter Cluster" \
	-p 9042:9042 \
	-p 9160:9160 \
	logimethods/smart-meter:cassandra$1
}

create_service_spark-master() {
docker service create \
	--name spark-master \
	-e SERVICE_NAME=spark-master \
	--network smart-meter-net \
	--replicas=${replicas} \
	--constraint 'node.role == manager' \
	--log-driver=json-file \
	gettyimages/spark:2.0.2-hadoop-2.7
}

create_service_spark-slave() {
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

create_service_app-batch() {
#docker pull logimethods/smart-meter:app-batch
docker service create \
	--name app-batch \
	-e SPARK_MASTER_URL=spark://spark-master:7077 \
	-e LOG_LEVEL=INFO \
	-e CASSANDRA_URL=$(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) \
	--network smart-meter-net \
	--replicas=${replicas} \
	logimethods/smart-meter:app-batch$1 
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

create_service_reporter() {
#docker pull logimethods/nats-reporter
docker service create \
	--name reporter \
	--network smart-meter-net \
	--replicas=${replicas} \
	-p 8888:8080 \
	logimethods/nats-reporter
}

create_cassandra_tables() {
# Create the Cassandra Tables
echo "Will create the Cassandra Messages Table"
until docker exec -it $(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) cqlsh -f '/cql/create-timeseries.cql'; do echo "Try again to create the Cassandra Time Series Table"; sleep 4; done
}

create_service_cassandra-inject() {
docker service create \
	--name cassandra-inject \
	--network smart-meter-net \
	--replicas=${replicas} \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e NATS_SUBJECT="smartmeter.voltage.data.>" \
	-e CASSANDRA_URL=$(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) \
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

update_service_scale() {
	docker service scale SERVICE=REPLICAS
}

### BUILDS ###

build_inject() {
	pushd dockerfile-inject
	sbt --warn update docker
	popd
}

build_app-streaming() {
	pushd dockerfile-app-streaming
	sbt --warn update docker
	popd
}

build_app-batch() {
	pushd dockerfile-app-batch
	sbt --warn update docker
	popd
}

build_monitor() {
	pushd dockerfile-monitor
	sbt --warn update docker
	popd
}

build_cassandra() {
	pushd dockerfile-cassandra
	docker build -t logimethods/smart-meter:cassandra-local .
	popd
}

build_cassandra-inject() {
	pushd dockerfile-cassandra-inject
	docker build -t logimethods/smart-meter:cassandra-inject-local .
	popd
}

build_nats-server() {
	pushd dockerfile-nats-server
	docker build -t logimethods/smart-meter:nats-server-local .
	popd
}

### WAIT ###

service_is_ready() {
	line=$(docker service ls | grep $1)
	tokens=( $line )
	replicas=${tokens[3]}
	actual=${replicas%%/*}
	expected=${replicas##*/}
	#echo "$actual : $expected"
	if [ "$actual" == "$expected" ] ; then
		return 0
	else
		return 1
	fi
}

wait_service() {
	echo "Waiting for the $1 Service to Start"
	until service_is_ready $1
	do 
		sleep 2
	done
}

### LOGS ###

logs_service() {
	docker logs $(docker ps | grep "$1" | rev | cut -d' ' -f1 | rev)
}

### Actual CMD ###

# See http://stackoverflow.com/questions/8818119/linux-how-can-i-run-a-function-from-a-script-in-command-line
echo "!!! $@ !!!"
"$@"
