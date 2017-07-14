#!/usr/bin/env bash
# https://github.com/docker/compose/issues/3435#issuecomment-232353235

echo "remote: $1"

. ./properties/configuration.properties

docker $1 stack rm "${STACK_NAME}"
