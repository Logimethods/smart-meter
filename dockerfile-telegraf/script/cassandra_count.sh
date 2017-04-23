#!/bin/sh

#renice 19 -p $$
docker exec $(docker ps | grep $CASSANDRA_URL | awk '{ print $1 }') cqlsh -e "select count(*) from ${TELEGRAF_CASSANDRA_TABLE};"
