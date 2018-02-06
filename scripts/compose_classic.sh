#!/bin/bash

docker pull logimethods/smart-meter:app_inject-1.0-dev
docker pull logimethods/smart-meter:app_compose-1.0-dev
docker pull logimethods/dz_telegraf:1.0-dev
docker pull logimethods/smart-meter:app_telegraf-1.0-dev
docker pull logimethods/smart-meter:app_monitor-1.0-dev
docker pull logimethods/smart-meter:app_cassandra-1.0-dev
docker pull logimethods/smart-meter:app_streaming-1.0-dev
docker pull logimethods/smart-meter:app_prometheus-1.0-dev

docker network create --attachable --driver overlay smartmeter
docker volume create grafana-volume

## docker stack rm smartmeter

docker run --rm -v `pwd`/alt_properties:/templater/alt_properties logimethods/smart-meter:app_compose-1.0-dev \
  combine_services -p alt_properties -e "local" "single" "no_secrets" \
  inject streaming_metrics prediction_metrics > docker-compose-merge.yml
#  inject streaming monitor > docker-compose-merge.yml
#  root_metrics inject streaming_metrics cassandra prediction_metrics > docker-compose-merge.yml
#  root_metrics inject streaming_metrics cassandra prediction_metrics > docker-compose-merge.yml
#  inject streaming_metrics cassandra prediction_metrics > docker-compose-merge.yml
#  inject_metrics streaming cassandra monitor prediction_metrics > docker-compose-merge.yml

docker-compose -f docker-compose-merge.yml up

docker-compose -f docker-compose-merge.yml down
