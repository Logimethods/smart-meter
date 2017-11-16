#!/bin/bash

docker network create --attachable --driver overlay deetazilla
docker stack rm deetazilla
docker pull logimethods/smart-meter:compose-1.0-dev
docker run --rm -v `pwd`/alt_properties:/templater/alt_properties logimethods/smart-meter:compose-1.0-dev combine_services -p alt_properties -e "local" "single" "no_secrets" inject streaming monitoring > docker-compose-merge.yml
docker stack deploy -c docker-compose-merge.yml deetazilla
