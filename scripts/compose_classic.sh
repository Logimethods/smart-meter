#!/bin/bash

#docker pull logimethods/smart-meter:app_streaming-1.0-dev
#docker pull logimethods/smart-meter:app_inject-1.0-dev
#docker pull logimethods/smart-meter:app_compose-1.0-dev

docker network create --attachable --driver overlay deetazilla
docker stack rm deetazilla

docker run --rm -v `pwd`/alt_properties:/templater/alt_properties logimethods/smart-meter:app_compose-1.0-dev \
  combine_services -p alt_properties -e "local" "single" "no_secrets" \
  inject streaming cassandra > docker-compose-merge.yml
#  inject streaming monitor > docker-compose-merge.yml

docker-compose -f docker-compose-merge.yml up

docker-compose -f docker-compose-merge.yml down
