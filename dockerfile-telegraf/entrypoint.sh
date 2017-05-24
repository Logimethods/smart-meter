#!/bin/sh
set -e

# An alternative to https://github.com/docker/swarm/issues/1106
# export DOCKER_TARGET_ID=$(docker ps | grep $DOCKER_TARGET_NAME | awk '{ print $1 }')
export DOCKER_TARGET_ID=$(wget -q -O - http://registry:5000/container/name/${DOCKER_TARGET_NAME}.${TASK_SLOT})
echo "TASK_SLOT: ${TASK_SLOT}"
echo "DOCKER_TARGET_ID: ${DOCKER_TARGET_ID}"

if [ "${1:0:1}" = '-' ]; then
    set -- telegraf "$@"
fi

exec "$@"
