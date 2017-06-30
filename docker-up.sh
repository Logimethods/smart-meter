#!/usr/bin/env bash
# https://github.com/docker/compose/issues/3435#issuecomment-232353235

echo "remote: $1"
echo "postfix: $2"

set -a
postfix="$2"
. ./properties/configuration.properties
set +a
docker $1 stack deploy -c docker-compose-test.yml "${STACK_NAME}"
