#!/bin/bash

replicas=1
postfix=""
shift_nb=0

while getopts ":r:p:" opt; do
  case $opt in
    r) replicas="$OPTARG"
    echo "replicas: $replicas"
    ((shift_nb+=2))
    ;;
    p) postfix="$OPTARG"
    echo "postfix: $postfix"
    ((shift_nb+=2))
    ;;
    \?) echo "Invalid option $OPTARG"
    ((shift_nb+=1))
    ;;
  esac
done

shift $shift_nb

# source the properties:
# https://coderanch.com/t/419731/read-properties-file-script
. configuration.properties

create_network() {
	docker network create --driver overlay --attachable smart-meter-net
#docker service rm $(docker service ls -q)
}

### Create Service ###

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
	logimethods/smart-meter:cassandra${postfix}
}

create_service_spark-master() {
docker service create \
	--name spark-master \
	-e SERVICE_NAME=spark-master \
	--network smart-meter-net \
	--replicas=${replicas} \
	--constraint 'node.role == manager' \
	--log-driver=json-file \
	${spark_image}:${spark_version}-hadoop-${hadoop_version}
}

create_service_spark-slave() {
docker service create \
	--name spark-slave \
	-e SERVICE_NAME=spark-slave \
	--network smart-meter-net \
	--replicas=${replicas} \
	${spark_image}:${spark_version}-hadoop-${hadoop_version} \
		bin/spark-class org.apache.spark.deploy.worker.Worker spark://spark-master:7077
}

create_service_nats() {
docker service create \
	--name nats \
	--network smart-meter-net \
	--replicas=${replicas} \
	-e NATS_USERNAME=${NATS_USERNAME} \
	-e NATS_PASSWORD=${NATS_PASSWORD} \
	logimethods/smart-meter:nats-server${postfix}
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
	logimethods/smart-meter:app-streaming${postfix} \
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
	logimethods/smart-meter:app-batch${postfix}
}

create_service_monitor() {
#docker pull logimethods/smart-meter:monitor
docker service create \
	--name monitor \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	--network smart-meter-net \
	--replicas=${replicas} \
	logimethods/smart-meter:monitor${postfix} \
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

create_service_cassandra-populate() {
docker service create \
	--name cassandra-inject \
	--network smart-meter-net \
	--replicas=${replicas} \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e NATS_SUBJECT="smartmeter.voltage.data.>" \
	-e CASSANDRA_URL=$(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) \
	logimethods/smart-meter:cassandra-populate${postfix}
}

create_service_inject() {
#docker pull logimethods/smart-meter:inject
docker service create \
	--name inject \
	-e GATLING_TO_NATS_SUBJECT=smartmeter.voltage.data \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	--network smart-meter-net \
	--replicas=${replicas} \
	logimethods/smart-meter:inject${postfix} \
		--no-reports -s com.logimethods.smartmeter.inject.NatsInjection
}


call_cassandra_cql() {
	until docker exec -it $(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) cqlsh -f "$1"; do echo "Try again to execute $1"; sleep 4; done
}

update_service_scale() {
	docker service scale SERVICE=REPLICAS
}

### RUN DOCKER ###

run_image() {
#	name=${1}
#	shift
	echo "docker run --network smart-meter-net $@"
	docker run --network smart-meter-net $@
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
  if [ "${postfix}" == "local" ]
  then
    ./set_properties_to_dockerfile_templates.sh
    pushd dockerfile-app-batch
    echo "docker build -t logimethods/smart-meter:app-batch-local ."
    sbt update assembly
    docker build -t logimethods/smart-meter:app-batch-local .
    popd
  else
    echo "docker pull logimethods/smart-meter:app-batch${postfix}"
    docker pull logimethods/smart-meter:app-batch${postfix}
  fi
}

build_monitor() {
	pushd dockerfile-monitor
	sbt --warn update docker
	popd
}

build_cassandra() {
  ./set_properties_to_dockerfile_templates.sh
	pushd dockerfile-cassandra
	docker build -t logimethods/smart-meter:cassandra-local .
	popd
}

build_cassandra-inject() {
  ./set_properties_to_dockerfile_templates.sh
	pushd dockerfile-cassandra-inject
	docker build -t logimethods/smart-meter:cassandra-inject-local .
	popd
}

build_nats-server() {
  ./set_properties_to_dockerfile_templates.sh
	pushd dockerfile-nats-server
	docker build -t logimethods/smart-meter:nats-server-local .
	popd
}

### WAIT ###

wait_service() {
	# http://unix.stackexchange.com/questions/213110/exiting-a-shell-script-with-nested-loops
	echo "Waiting for the $1 Service to Start"
	while :
	do
		echo "--------- $1 ----------"
		docker ps | while read -r line
		do
			tokens=( $line )
			full_name=${tokens[1]}
			name=${full_name##*:}
			if [ "$name" == "$1" ] ; then
				exit 1
			fi
		done
		[[ $? != 0 ]] && exit 0

		docker service ls | while read -r line
		do
			tokens=( $line )
			name=${tokens[1]}
			if [ "$name" == "$1" ] ; then
				replicas=${tokens[3]}
				actual=${replicas%%/*}
				expected=${replicas##*/}
				#echo "$actual : $expected"
				if [ "$actual" == "$expected" ] ; then
					exit 1
				else
					break
				fi
			fi
		done
		[[ $? != 0 ]] && exit 0

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
