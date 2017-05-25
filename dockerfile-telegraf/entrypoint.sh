#!/bin/sh
set -e

# An alternative to https://github.com/docker/swarm/issues/1106
# export DOCKER_TARGET_ID=$(docker ps | grep $DOCKER_TARGET_NAME | awk '{ print $1 }')
#
if [ -z "$NODE_ID" ]
then
    export DOCKER_TARGET_ID=$(wget -q -O - http://registry:5000/container/name/${DOCKER_TARGET_NAME}.)
else
    export DOCKER_TARGET_ID=$(wget -q -O - http://registry:5000/service/name/${NODE_ID}/${DOCKER_TARGET_NAME})
fi

echo "NODE_ID: ${NODE_ID}"
echo "DOCKER_TARGET_ID: ${DOCKER_TARGET_ID}"
if [ $TELEGRAF_DEBUG = "true" ]; then env; fi

if [ "${1:0:1}" = '-' ]; then
    set -- telegraf "$@"
fi

exec "$@"
