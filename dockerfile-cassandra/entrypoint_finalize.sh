#!/bin/bash

if [ -f ${CASSANDRA_SETUP_FILE} ] && [ ! -n "${CASSANDRA_SEEDS}" ]; then
  echo "Will initialize Cassandra through ${CASSANDRA_SETUP_FILE}"
  cqlsh -f "${CASSANDRA_SETUP_FILE}"
fi

# Export the raw_data count
# https://stackoverflow.com/questions/16640054/minimal-web-server-using-netcat
if [ -n "${CASSANDRA_COUNT_PORT}" ]; then
  (while true; do cqlsh -e "select * from smartmeter.raw_data_count ;" | nc -l 6161 >/dev/null; done) &
fi
