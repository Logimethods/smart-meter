#!/bin/bash
set -e

source eureka_utils.sh

# An alternative to https://github.com/docker/swarm/issues/1106

: ${EUREKA_URL:=eureka}
: ${EUREKA_PORT:=5000}

setup_local_containers

if [ $TELEGRAF_DEBUG = "true" ]; then env; fi

if [ "${1:0:1}" = '-' ]; then
    set -- telegraf "$@"
fi

exec "$@"
