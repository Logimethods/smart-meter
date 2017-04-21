#!/bin/bash

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"

replicas=1
postfix=""
shift_nb=0

while getopts ":r:p:" opt; do
  case $opt in
    r) replicas="$OPTARG"
    # echo "replicas: $replicas"
    ((shift_nb+=2))
    ;;
    p) postfix="$OPTARG"
    # echo "postfix: $postfix"
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

#NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222
NATS_URI=nats://nats:4222
#CASSANDRA_URL=$(docker ${remote} ps | grep "${CASSANDRA_MAIN_NAME}" | rev | cut -d' ' -f1 | rev)
CASSANDRA_URL=$CASSANDRA_MAIN_NAME

if [ "${postfix}" == "-remote" ]
then
  postfix=""
  remote=" -H localhost:2374 "
  CASSANDRA_URL=$CASSANDRA_NODE_NAME
  # echo "Will use a REMOTE Docker Cluster"
fi

# echo "CASSANDRA_URL: ${CASSANDRA_URL}"

stop_all() {
  docker ${remote} service rm $(docker ${remote} service ls -q)
  docker ${remote} stop $(docker ${remote} ps | grep -v aws | cut -d ' ' -f 1)
}

create_network() {
	docker ${remote} network create --driver overlay --attachable smartmeter
#docker ${remote} service rm $(docker ${remote} service ls -q)
}

### Cassandra ###

create_volume_cassandra() {
  if [ "${postfix}" == "-remote" ]
  then
    cassandra_size=$CASSANDRA_REMOTE_VOLUME_SIZE
  elif [[ "${postfix}" == "-local" ]]; then
    cassandra_size=$CASSANDRA_LOCAL_VOLUME_SIZE
  else
    cassandra_size=$CASSANDRA_DEFAULT_VOLUME_SIZE
  fi

  docker ${remote} volume create --name cassandra-volume-1
#  docker ${remote} volume create --name cassandra-volume-2 --opt o=size=$cassandra_size
#  docker ${remote} volume create --name cassandra-volume-3 --opt o=size=$cassandra_size
}

create_cluster_cassandra() {
docker-compose ${remote} -f docker-cassandra-compose.yml up -d
}

kill_cluster_cassandra() {
docker-compose ${remote} -f docker-cassandra-compose.yml down
}

call_cassandra_cql() {
	until docker ${remote} exec -it $(docker ${remote} ps | grep "${CASSANDRA_MAIN_NAME}" | rev | cut -d' ' -f1 | rev) cqlsh -f "$1"; do echo "Try again to execute $1"; sleep 4; done
  # docker ${remote} run ${DOCKER_RESTART_POLICY} --net=smartmeter logimethods/smart-meter:cassandra sh -c 'exec cqlsh "cassandra-1" -f "$1"'
}

run_cassandra() {
  cmd="docker ${remote} run -d ${DOCKER_RESTART_POLICY} \
    --name ${CASSANDRA_MAIN_NAME} \
  	--network smartmeter \
    -p 8778:8778 \
    -e LOCAL_JMX=no \
    -v cassandra-volume-1:/var/lib/cassandra \
  	logimethods/smart-meter:cassandra${postfix}"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

create_service_cassandra() {
  # https://hub.docker.com/_/cassandra/
  # http://serverfault.com/questions/806649/docker-swarm-and-volumes
  # https://clusterhq.com/2016/03/09/fun-with-swarm-part1/
  # https://github.com/Yannael/kafka-sparkstreaming-cassandra-swarm/blob/master/service-management/start-cassandra-services.sh

  docker ${remote} service create \
  	--name ${CASSANDRA_MAIN_NAME} \
  	--network smartmeter \
    --constraint 'node.role == manager' \
    -e LOCAL_JMX=no \
  	logimethods/smart-meter:cassandra${postfix}

  #Need to sleep a bit so IP can be retrieved below
  while [[ -z $(docker ${remote} service ls |grep ${CASSANDRA_MAIN_NAME}| grep 1/1) ]]; do
  	Echo Waiting for Cassandra seed service to start...
  	sleep 2
  	done;

  export CASSANDRA_SEED="$(docker ${remote} ps |grep ${CASSANDRA_MAIN_NAME}|cut -d ' ' -f 1)"
  echo "CASSANDRA_SEED: $CASSANDRA_SEED"

  docker ${remote} service create \
  	--name ${CASSANDRA_NODE_NAME} \
  	--network smartmeter \
    --mode global \
    --constraint 'node.role != manager' \
    --env CASSANDRA_SEEDS=$CASSANDRA_SEED \
  	logimethods/smart-meter:cassandra${postfix}
}

create_full_service_cassandra() {
# https://hub.docker.com/_/cassandra/
# http://serverfault.com/questions/806649/docker-swarm-and-volumes
# https://clusterhq.com/2016/03/09/fun-with-swarm-part1/
docker ${remote} service create \
	--name ${CASSANDRA_MAIN_NAME} \
	--network smartmeter \
	--mount type=volume,source=cassandra-volume-1,destination=/var/lib/cassandra \
  --constraint 'node.role == manager' \
	-e CASSANDRA_BROADCAST_ADDRESS="cassandra" \
	-e CASSANDRA_CLUSTER_NAME="Smartmeter Cluster" \
	-p 9042:9042 \
	-p 9160:9160 \
	logimethods/smart-meter:cassandra${postfix}
}

create_service_spark-master() {
docker ${remote} service create \
	--name spark-master \
	-e SERVICE_NAME=spark-master \
	--network smartmeter \
	--replicas=${replicas} \
	--constraint 'node.role == manager' \
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

run_spark_autoscaling() {
  cmd="docker ${remote} run -d ${DOCKER_RESTART_POLICY} \
    -v /var/run/docker.sock:/var/run/docker.sock \
    --name spark_autoscaling \
    --network smartmeter \
    logimethods/spark-autoscaling python3 autoscale_sh.py"
#    --log-driver=json-file \
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

### Create Service ###

create_service_nats() {
docker ${remote} service create \
	--name nats \
	--network smartmeter \
	--replicas=${replicas} \
	-e NATS_USERNAME=${NATS_USERNAME} \
	-e NATS_PASSWORD=${NATS_PASSWORD} \
  --constraint 'node.role == manager' \
  -p 4222:4222 \
  -p 8222:8222 \
	logimethods/smart-meter:nats-server${postfix}  -m 8222
}

create_service_app_streaming() {
#docker ${remote} pull logimethods/smart-meter:app-streaming
docker ${remote} service create \
	--name app_streaming \
	-e NATS_URI=${NATS_URI} \
	-e SPARK_MASTER_URL=${SPARK_MASTER_URL_STREAMING} \
  -e STREAMING_DURATION=${STREAMING_DURATION} \
  -e CASSANDRA_URL=${CASSANDRA_URL} \
	-e LOG_LEVEL=${APP_STREAMING_LOG_LEVEL} \
  --replicas=1 \
  --constraint 'node.role == manager' \
	--network smartmeter \
	logimethods/smart-meter:app-streaming${postfix}  "com.logimethods.nats.connector.spark.app.SparkMaxProcessor" \
		"smartmeter.voltage.raw.>" "smartmeter.voltage.extract.max" \
    "Smartmeter MAX Streaming"

#   --mode global \
#    --replicas=${replicas} \
#    --constraint 'node.role == manager' \
}

__run_app_streaming() {
#docker ${remote} pull logimethods/smart-meter:app-streaming
  cmd="docker ${remote} run --rm -d \
  	--name app_streaming \
  	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
    -e CASSANDRA_URL=${CASSANDRA_URL} \
  	-e SPARK_MASTER_URL=${SPARK_MASTER_URL_STREAMING} \
    -e STREAMING_DURATION=${STREAMING_DURATION} \
  	-e LOG_LEVEL=${APP_STREAMING_LOG_LEVEL} \
  	--network smartmeter \
  	logimethods/smart-meter:app-streaming${postfix}  com.logimethods.nats.connector.spark.app.SparkMaxProcessor \
  		\"smartmeter.voltage.raw.forecast.12\" \"smartmeter.voltage.extract.prediction.12\" \
      \"Smartmeter MAX Streaming\" "
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec "$cmd"
}

create_service_app_prediction() {
#docker ${remote} pull logimethods/smart-meter:app-streaming
docker ${remote} service create \
	--name app_prediction \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e SPARK_MASTER_URL=${SPARK_MASTER_URL_STREAMING} \
  -e CASSANDRA_URL=${CASSANDRA_URL} \
	-e LOG_LEVEL=INFO \
  -e ALERT_THRESHOLD=${ALERT_THRESHOLD} \
	--network smartmeter \
  --replicas=${replicas} \
	logimethods/smart-meter:app-streaming${postfix}  "com.logimethods.nats.connector.spark.app.SparkPredictionProcessor" \
		"smartmeter.voltage.raw.forecast.12" "smartmeter.voltage.extract.prediction.12" \
    "Smartmeter PREDICTION Streaming"

#    --replicas=${replicas} \
#    --constraint 'node.role == manager' \
}

run_app_prediction() {
#docker ${remote} pull logimethods/smart-meter:app-streaming
  cmd="docker ${remote} run --rm \
  	--name app_prediction \
  	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
  	-e SPARK_MASTER_URL=${SPARK_MASTER_URL_STREAMING} \
    -e CASSANDRA_URL=${CASSANDRA_URL} \
  	-e LOG_LEVEL=INFO \
    -e ALERT_THRESHOLD=${ALERT_THRESHOLD} \
  	--network smartmeter \
  	logimethods/smart-meter:app-streaming${postfix}  com.logimethods.nats.connector.spark.app.SparkPredictionProcessor \
  		\"smartmeter.voltage.raw.forecast.12\" \"smartmeter.voltage.extract.prediction.12\" \
      \"Smartmeter PREDICTION Streaming\" "
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

run_app-batch() {
  #docker ${remote} pull logimethods/smart-meter:inject
  cmd="docker ${remote} run --rm \
    --name app_batch \
  	-e SPARK_MASTER_URL=${SPARK_MASTER_URL_BATCH} \
    -e CASSANDRA_URL=${CASSANDRA_URL} \
    -e APP_BATCH_LOG_LEVEL=${APP_BATCH_LOG_LEVEL} \
    --network smartmeter \
    logimethods/smart-meter:app-batch${postfix}"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

create_service_app-batch() {
#docker ${remote} pull logimethods/smart-meter:app-batch
docker ${remote} service create \
	--name app-batch \
	-e SPARK_MASTER_URL=${SPARK_MASTER_URL_BATCH} \
	-e LOG_LEVEL=INFO \
	-e CASSANDRA_URL=${CASSANDRA_URL} \
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
docker ${remote} service create \
	--name cassandra-inject \
	--network smartmeter \
  --mode global \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
	-e NATS_SUBJECT="smartmeter.voltage.raw.data.>" \
	-e CASSANDRA_URL=${CASSANDRA_URL} \
	logimethods/smart-meter:cassandra-inject${postfix}

# 	--replicas=${replicas} \
}

create_service_inject() {

echo "GATLING_USERS_PER_SEC: ${GATLING_USERS_PER_SEC}"
echo "GATLING_DURATION: ${GATLING_DURATION}"

#docker ${remote} pull logimethods/smart-meter:inject
cmd="docker ${remote} service create \
	--name inject \
	-e GATLING_TO_NATS_SUBJECT=smartmeter.voltage.raw \
	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
  -e GATLING_USERS_PER_SEC=${GATLING_USERS_PER_SEC} \
  -e GATLING_DURATION=${GATLING_DURATION} \
  -e STREAMING_DURATION=${STREAMING_DURATION} \
  -e SERVICE_ID={{.Service.ID}} \
  -e SERVICE_NAME={{.Service.Name}} \
  -e SERVICE_LABELS={{.Service.Labels}} \
  -e TASK_ID={{.Task.ID}} \
  -e TASK_NAME={{.Task.Name}} \
  -e TASK_SLOT={{.Task.Slot}} \
  -e RANDOMNESS=${VOLTAGE_RANDOMNESS} \
  -e PREDICTION_LENGTH=${PREDICTION_LENGTH} \
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
  	-e GATLING_TO_NATS_SUBJECT=smartmeter.voltage.raw \
  	-e NATS_URI=nats://${NATS_USERNAME}:${NATS_PASSWORD}@nats:4222 \
    -e GATLING_USERS_PER_SEC=${GATLING_USERS_PER_SEC} \
    -e GATLING_DURATION=${GATLING_DURATION} \
    -e STREAMING_DURATION=${STREAMING_DURATION} \
    -e TASK_SLOT=1 \
    -e RANDOMNESS=${VOLTAGE_RANDOMNESS} \
    -e PREDICTION_LENGTH=${PREDICTION_LENGTH} \
  	--network smartmeter \
  	logimethods/smart-meter:inject${postfix} \
		--no-reports -s com.logimethods.smartmeter.inject.NatsInjection"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

run_metrics() {
  # https://bronhaim.wordpress.com/2016/07/24/setup-toturial-for-collecting-metrics-with-statsd-and-grafana-containers/
  run_metrics_graphite
  run_metrics_grafana
}

run_metrics_graphite() {
  if [ "${postfix}" == "-local" ]
  then
    local_conf="-v ${METRICS_PATH}/graphite/conf:/opt/graphite/conf"
  fi

  cmd="docker ${remote} run -d ${DOCKER_RESTART_POLICY} \
  --network smartmeter \
  --name metrics \
  $local_conf \
  -p 81:80 \
  hopsoft/graphite-statsd:${graphite_statsd_tag}"
  # -v ${METRICS_PATH}/graphite/storage:/opt/graphite/storage\
  # -v ${METRICS_PATH}/statsd:/opt/statsd\
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  sh -c "$cmd"
}

create_volume_grafana() {
  ## https://github.com/grafana/grafana-docker#grafana-container-with-persistent-storage-recommended
  #docker ${remote} run -d -v /var/lib/grafana --name grafana-storage --network smartmeter busybox:latest

  docker ${remote} volume create --name grafana-volume
}

run_metrics_grafana() {
  cmd="docker ${remote} run -d ${DOCKER_RESTART_POLICY}\
  --network smartmeter \
  --name grafana \
  -p ${METRICS_GRAFANA_WEB_PORT}:3000 \
  -e \"GF_SERVER_ROOT_URL=http://localhost:3000\" \
  -e \"GF_SECURITY_ADMIN_PASSWORD=${GF_SECURITY_ADMIN_PASSWORD}\" \
  -v grafana-volume:/var/lib/grafana \
  grafana/grafana:${grafana_tag}"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  sh -c "$cmd"
}

update_service_scale() {
	docker ${remote} service scale SERVICE=REPLICAS
}

run_telegraf() {
   if [ "$@" == "docker" ]
     then DOCKER_ACCES="-v /var/run/docker.sock:/var/run/docker.sock"
   fi
   cmd="docker ${remote} run -d ${DOCKER_RESTART_POLICY}\
     --network smartmeter \
     --name telegraf_$@\
     -e CASSANDRA_URL=${CASSANDRA_MAIN_NAME} \
     -e JMX_PASSWORD=$JMX_PASSWORD \
     -e TELEGRAF_DEBUG=$TELEGRAF_DEBUG \
     -e TELEGRAF_QUIET=$TELEGRAF_QUIET \
     --log-driver=json-file \
     $DOCKER_ACCES \
     logimethods/smart-meter:telegraf${postfix}\
       telegraf -config /etc/telegraf/$@.conf"
    echo "-----------------------------------------------------------------"
    echo "$cmd"
    echo "-----------------------------------------------------------------"
    exec $cmd
}

run_service_telegraf_docker() {
  cmd="docker ${remote} service create \
  	--name telegraf_docker \
  	--network smartmeter \
  	--mode global \
    --mount type=bind,source=/var/run/docker.sock,destination=/var/run/docker.sock \
    logimethods/smart-meter:telegraf${postfix}\
      telegraf -config /etc/telegraf/docker.conf"
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  exec $cmd
}

### ZEPPELIN ###

run_zeppelin() {
  cmd="docker ${remote} run -d --rm\
  --network smartmeter \
  --name zeppelin \
  -p ${ZEPPELIN_WEB_PORT}:8080 \
  dylanmei/zeppelin:${zeppelin_tag} sh -c \"./bin/install-interpreter.sh --name cassandra ; ./bin/zeppelin.sh\""
  echo "-----------------------------------------------------------------"
  echo "$cmd"
  echo "-----------------------------------------------------------------"
  sh -c "$cmd"
}

### RUN DOCKER ###

run_image() {
#	name=${1}
#	shift
	echo "docker ${remote} run --network smartmeter ${DOCKER_RESTART_POLICY} $@"
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
  if [ "${postfix}" == "-local" ]
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

build_telegraf() {
  ./set_properties_to_dockerfile_templates.sh
	pushd dockerfile-telegraf
  docker build -t logimethods/smart-meter:telegraf-local .
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
