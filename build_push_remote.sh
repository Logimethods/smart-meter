#!/bin/bash

source build.sh

build_dockerfile_inject ""
docker push logimethods/smart-meter:inject

build_dockerfile_app_streaming ""
docker push logimethods/smart-meter:app-streaming

build_dockerfile_app_batch ""
docker push logimethods/smart-meter:app-batch

build_dockerfile_prometheus ""
docker push logimethods/smart-meter:prometheus

build_dockerfile_monitor ""
docker push logimethods/smart-meter:monitor

build_dockerfile_cassandra ""
docker push logimethods/smart-meter:cassandra

build_dockerfile_telegraf ""
docker push logimethods/smart-meter:telegraf

build_dockerfile_cassandra_inject ""
docker push logimethods/smart-meter:cassandra-inject

build_dockerfile_nats_server ""
docker push logimethods/smart-meter:nats-server

#docker push logimethods/prometheus-nats-exporter
#docker push logimethods/service-registry
