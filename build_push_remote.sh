#!/bin/bash

source build.sh

pushd ../docker-eureka
./build_exp.sh
popd

extension=""

clear
echo "-----------------------------------------"
echo "build_dockerfile_inject $extension"
build_dockerfile_inject ""
docker push logimethods/smart-meter:inject
docker -H localhost:2374 pull logimethods/smart-meter:inject

clear
echo "-----------------------------------------"
echo "build_dockerfile_app_streaming $extension"
build_dockerfile_app_streaming ""
docker push logimethods/smart-meter:app-streaming
docker -H localhost:2374 pull logimethods/smart-meter:app-streaming

clear
echo "-----------------------------------------"
echo "build_dockerfile_app_batch $extension"
build_dockerfile_app_batch ""
docker push logimethods/smart-meter:app-batch
docker -H localhost:2374 pull logimethods/smart-meter:app-batch

clear
echo "-----------------------------------------"
echo "build_dockerfile_prometheus $extension"
build_dockerfile_prometheus ""
docker push logimethods/smart-meter:prometheus
docker -H localhost:2374 pull logimethods/smart-meter:prometheus

clear
echo "-----------------------------------------"
echo "build_dockerfile_monitor $extension"
build_dockerfile_monitor ""
docker push logimethods/smart-meter:monitor
docker -H localhost:2374 pull logimethods/smart-meter:monitor

clear
echo "-----------------------------------------"
echo "build_dockerfile_cassandra $extension"
build_dockerfile_cassandra ""
docker push logimethods/smart-meter:cassandra
docker -H localhost:2374 pull logimethods/smart-meter:cassandra

clear
echo "-----------------------------------------"
echo "build_dockerfile_telegraf $extension"
build_dockerfile_telegraf ""
docker push logimethods/smart-meter:telegraf
docker -H localhost:2374 pull logimethods/smart-meter:telegraf

clear
echo "-----------------------------------------"
echo "build_dockerfile_cassandra_inject $extension"
build_dockerfile_cassandra_inject ""
docker push logimethods/smart-meter:cassandra-inject
docker -H localhost:2374 pull logimethods/smart-meter:cassandra-inject

clear
echo "-----------------------------------------"
echo "build_dockerfile_nats_server $extension"
build_dockerfile_nats_server ""
docker push logimethods/smart-meter:nats-server
docker -H localhost:2374 pull logimethods/smart-meter:nats-server

#docker push logimethods/prometheus-nats-exporter
#docker push logimethods/service-registry
