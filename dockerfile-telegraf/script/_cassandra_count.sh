#!/bin/sh

#renice 19 -p $$
docker exec ${DOCKER_TARGET_ID} cqlsh -e "select count(*) from ${TELEGRAF_CASSANDRA_TABLE};" | head -4 | tail -1
