#!/bin/sh
set -e

# An alternative to https://github.com/docker/swarm/issues/1106
export DOCKER_TARGET_ID=$(docker ps | grep $DOCKER_TARGET_NAME | awk '{ print $1 }')

if [ "${1:0:1}" = '-' ]; then
    set -- telegraf "$@"
fi

exec "$@"
