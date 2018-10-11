#!/usr/bin/env bash
# https://github.com/docker/compose/issues/3435#issuecomment-232353235

set -a
location="$1"
cluster_mode="$2"
postfix="$3"

echo "location: $location"
echo "cluster_mode: $cluster_mode"
echo "postfix: $postfix"

source properties/configuration.properties
source "properties/configuration-location-${location}.properties"
source "properties/configuration-location-${location}-debug.properties"
source "properties/configuration-mode-${cluster_mode}.properties"
source "properties/configuration-mode-${cluster_mode}-debug.properties"
source "properties/configuration-telegraf.properties"
source "properties/configuration-telegraf-debug.properties"
set +a

echo "DOCKER_COMPOSE_FILE: ${DOCKER_COMPOSE_FILE}"

echo "If not already done, create the required network: > docker ${remote} network create --driver overlay --attachable ${NETWORK_NAME}"

# DOCKER_ORCHESTRATOR=swarm
docker ${remote} stack deploy -c ${DOCKER_COMPOSE_FILE} "${STACK_NAME}"
