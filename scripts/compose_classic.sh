#!/bin/bash

docker_tag="1.5"

docker pull logimethods/smart-meter:app_inject-${docker_tag}
docker pull logimethods/smart-meter:app_compose-${docker_tag}
docker pull logimethods/dz_telegraf:${docker_tag}
docker pull logimethods/smart-meter:app_telegraf-${docker_tag}
docker pull logimethods/smart-meter:app_monitor-${docker_tag}
docker pull logimethods/smart-meter:app_cassandra-${docker_tag}
docker pull logimethods/smart-meter:app_streaming-${docker_tag}
docker pull logimethods/smart-meter:app_prometheus-${docker_tag}

docker network create --attachable --driver overlay smartmeter
docker volume create grafana-volume

## docker stack rm smartmeter

docker run --rm -v `pwd`/alt_properties:/templater/alt_properties logimethods/smart-meter:app_compose-${docker_tag} \
  combine_services -p alt_properties -e "local" "single" "no_secrets" \
  root_metrics inject streaming_metrics prediction_metrics > docker-compose-merge.yml
#  inject streaming monitor > docker-compose-merge.yml
#  root_metrics inject streaming_metrics cassandra prediction_metrics > docker-compose-merge.yml
#  root_metrics inject streaming_metrics cassandra prediction_metrics > docker-compose-merge.yml
#  inject streaming_metrics cassandra prediction_metrics > docker-compose-merge.yml
#  inject_metrics streaming cassandra monitor prediction_metrics > docker-compose-merge.yml

docker-compose -f docker-compose-merge.yml up

docker-compose -f docker-compose-merge.yml down
