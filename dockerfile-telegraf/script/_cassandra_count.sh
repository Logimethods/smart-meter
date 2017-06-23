#!/bin/sh

#renice 19 -p $$
docker exec ${CASSANDRA_LOCAL_URL} cqlsh -e "select count(*) from ${TELEGRAF_CASSANDRA_TABLE};" | head -4 | tail -1
