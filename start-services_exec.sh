#!/bin/bash

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"

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

if [ "${postfix}" == "-remote" ]
then
  postfix=""
  remote=" -H localhost:2374 "
  echo "Will use a REMOTE Docker Cluster"
fi

# source the properties:
# https://coderanch.com/t/419731/read-properties-file-script
. configuration.properties

create_network() {
	docker ${remote} network create --driver overlay --attachable smartmeter
#docker ${remote} service rm $(docker ${remote} service ls -q)
}

### Cassandra ###

create_volumes_cassandra() {
  docker ${remote} volume create --name cassandra-volume-1 -o size=10G
  docker ${remote} volume create --name cassandra-volume-2 -o size=10G
  docker ${remote} volume create --name cassandra-volume-3 -o size=10G
}

create_cluster_cassandra() {
docker-compose ${remote} -f docker-cassandra-compose.yml up -d
}

kill_cluster_cassandra() {
docker-compose ${remote} -f docker-cassandra-compose.yml down
}

call_cassandra_cql() {
	until docker ${remote} exec -it $(docker ${remote} ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) cqlsh -f "$1"; do echo "Try again to execute $1"; sleep 4; done
  # docker ${remote} run --rm --net=smartmeter logimethods/smart-meter:cassandra sh -c 'exec cqlsh "cassandra-1" -f "$1"'
}

create_service_cassandra() {
# https://hub.docker.com/_/cassandra/
# http://serverfault.com/questions/806649/docker-swarm-and-volumes
# https://clusterhq.com/2016/03/09/fun-with-swarm-part1/
docker ${remote} service create \
	--name cassandra \
	--network smartmeter \
	--mount type=volume,source=cassandra-volume-1,destination=/var/lib/cassandra \
  --constraint 'node.role == manager' \
	-e CASSANDRA_BROADCAST_ADDRESS="cassandra" \
	-e CASSANDRA_CLUSTER_NAME="Smartmeter Cluster" \
	-p 9042:9042 \
	-p 9160:9160 \
	logimethods/smart-meter:cassandra${postfix}
}

### Create Service ###

create_service_spark-master() {
docker ${remote} service create \
	--name spark-master \
	-e SERVICE_NAME=spark-master \
	--network smartmeter \
	--replicas=${replicas} \
	--constraint 'node.role == manager' \
	--log-driver=json-file \
	${spark_image}:${spark_version}-hadoop-${hadoop_version}
}

create_service_spark-slave() {
docker ${remote} service create \
	--name spark-slave \
	-e SERVICE_NAME=spark-slave \
	--network smartmeter \
	--replicas=${replicas} \
	${spark_image}:${spark_version}-hadoop-${hadoop_version} \
		bin/spark-class org.apache.spark.deploy.worker.Worker spark://spark-master:7077
}

create_service_nats() {
docker ${remote} service create \
	--name nats \
	--network smartmeter \
	--replicas=${replicas} \
	-e NATS_USERNAME=${NATS_USERNAME} \
	-e NATS_PASSWORD=${NATS_PASSWORD} \
  -p 4222:4222 \
  -p 8222:8222 \
	logimethods/smart-meter:nats-server${postfix}  -m 8222
}

create_service_app-streaming() {
#docker ${remote} pull logimethods/smart-meter:app-streaming
docker ${remote} service create \
	--name app-streaming \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e SPARK_MASTER_URL=spark://spark-master:7077 \
	-e LOG_LEVEL=INFO \
	--network smartmeter \
	--replicas=${replicas} \
	logimethods/smart-meter:app-streaming${postfix} \
		"smartmeter.voltage.data.>" "smartmeter.voltage.data. => smartmeter.voltage.extract.max."
}

create_service_app-batch() {
#docker ${remote} pull logimethods/smart-meter:app-batch
docker ${remote} service create \
	--name app-batch \
	-e SPARK_MASTER_URL=spark://spark-master:7077 \
	-e LOG_LEVEL=INFO \
	-e CASSANDRA_URL=$(docker ${remote} ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) \
	--network smartmeter \
	--replicas=${replicas} \
	logimethods/smart-meter:app-batch${postfix}
}

create_service_monitor() {
#docker ${remote} pull logimethods/smart-meter:monitor
docker ${remote} service create \
	--name monitor \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	--network smartmeter \
	--replicas=${replicas} \
	logimethods/smart-meter:monitor${postfix} \
		"smartmeter.voltage.extract.>"
}

create_service_reporter() {
#docker ${remote} pull logimethods/nats-reporter
docker ${remote} service create \
	--name reporter \
	--network smartmeter \
	--replicas=${replicas} \
	-p 8888:8080 \
	logimethods/nats-reporter
}

create_service_cassandra-inject() {
CASSANDRA_URL=$(docker ${remote} ps | grep "cassandra.1" | rev | cut -d' ' -f1 | rev)
echo "CASSANDRA_URL: ${CASSANDRA_URL}"
docker ${remote} service create \
	--name cassandra-inject \
	--network smartmeter \
	--replicas=${replicas} \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e NATS_SUBJECT="smartmeter.voltage.data.>" \
	-e CASSANDRA_URL=${CASSANDRA_URL} \
	logimethods/smart-meter:cassandra-inject${postfix}
}

create_service_inject() {

echo "GATLING_USERS_PER_SEC: ${GATLING_USERS_PER_SEC}"
echo "GATLING_DURATION: ${GATLING_DURATION}"

#docker ${remote} pull logimethods/smart-meter:inject
cmd="docker ${remote} service create \
	--name inject \
	-e GATLING_TO_NATS_SUBJECT=smartmeter.voltage.data \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
  -e GATLING_USERS_PER_SEC=${GATLING_USERS_PER_SEC} \
  -e GATLING_DURATION=${GATLING_DURATION} \
  -e SERVICE_ID={{.Service.ID}}
  -e SERVICE_NAME={{.Service.Name}}
  -e SERVICE_LABELS={{.Service.Labels}}
  -e TASK_ID={{.Task.ID}}
  -e TASK_NAME={{.Task.Name}}
  -e TASK_SLOT={{.Task.Slot}}
	--network smartmeter \
	--replicas=${replicas} \
	logimethods/smart-meter:inject${postfix} \
		--no-reports -s com.logimethods.smartmeter.inject.NatsInjection"
echo "-----------------------------------------------------------------"
echo "$cmd"
echo "-----------------------------------------------------------------"
eval $cmd
}

run_inject() {
  echo "GATLING_USERS_PER_SEC: ${GATLING_USERS_PER_SEC}"
  echo "GATLING_DURATION: ${GATLING_DURATION}"
  echo "Replicas: $@"

  #docker ${remote} pull logimethods/smart-meter:inject
  cmd="docker ${remote} run \
    -it \
  	-e GATLING_TO_NATS_SUBJECT=smartmeter.voltage.data \
  	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
    -e GATLING_USERS_PER_SEC=${GATLING_USERS_PER_SEC} \
    -e GATLING_DURATION=${GATLING_DURATION} \
    -e TASK_SLOT=1
  	--network smartmeter \
  	logimethods/smart-meter:inject${postfix} \
		--no-reports -s com.logimethods.smartmeter.inject.NatsInjection"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

run_metrics() {
  run_metrics_grafana
}

run_metrics_grafana() {
  cmd="docker ${remote} run -d \
  -p ${METRICS_GRAFANA_WEB_PORT}:80 -p ${METRICS_GRAPHITE_WEB_PORT}:81 \
  -p 8125:8125/udp -p 8126:8126 \
  --network smartmeter \
  --name metrics \
  kamon/grafana_graphite"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

run_metrics_graphite() {
  cmd="docker ${remote} run -d\
   --name metrics\
   --restart=always\
   --network smartmeter \
   -p ${METRICS_WEB_PORT}:80\
   -p 2003-2004:2003-2004\
   -p 2023-2024:2023-2024\
   -p 8125:8125/udp\
   -p 8126:8126\
   hopsoft/graphite-statsd"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

update_service_scale() {
	docker ${remote} service scale SERVICE=REPLICAS
}

run_telegraf() {
   cmd="docker ${remote} run -d\
     --network smartmeter \
     --name telegraf\
     logimethods/smart-meter:telegraf${postfix}\
       telegraf -config /etc/telegraf/$@.conf"
    echo "-----------------------------------------------------------------"
    echo "$cmd"
    echo "-----------------------------------------------------------------"
    exec $cmd
}

### RUN DOCKER ###

run_image() {
#	name=${1}
#	shift
	echo "docker ${remote} run --network smartmeter $@"
	docker ${remote} run --network smartmeter $@
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
    mkdir -p libs
    mv target/scala-*/*.jar libs/
    docker build -t logimethods/smart-meter:app-batch-local .
    popd
  else
    echo "docker ${remote} pull logimethods/smart-meter:app-batch${postfix}"
    docker ${remote} pull logimethods/smart-meter:app-batch${postfix}
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

### SCALE ###

scale_service() {
  cmd="docker ${remote} service scale $1"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

### RM ###

rm_service() {
  cmd="docker ${remote} service rm $1"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

### WAIT ###

wait_service() {
	# http://unix.stackexchange.com/questions/213110/exiting-a-shell-script-with-nested-loops
	echo "Waiting for the $1 Service to Start"
	while :
	do
		echo "--------- $1 ----------"
		docker ${remote} ps | while read -r line
		do
			tokens=( $line )
			full_name=${tokens[1]}
			name=${full_name##*:}
			if [ "$name" == "$1" ] ; then
				exit 1
			fi
		done
		[[ $? != 0 ]] && exit 0

		docker ${remote} service ls | while read -r line
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
	docker ${remote} logs $(docker ${remote} ps | grep "$1" | rev | cut -d' ' -f1 | rev)
}

### Actual CMD ###

# See http://stackoverflow.com/questions/8818119/linux-how-can-i-run-a-function-from-a-script-in-command-line
echo "!!! $@ !!!"
"$@"

# echo "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
