#!/usr/bin/env bash
# https://github.com/docker/compose/issues/3435#issuecomment-232353235
set -a
postfix=-local
. ./properties/configuration.properties
set +a
docker stack deploy -c docker-compose.yml "${STACK_NAME}"
