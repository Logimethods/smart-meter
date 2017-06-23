#!/bin/bash

if [ -f ${CASSANDRA_SETUP_FILE} ] && [ ! -n "${CASSANDRA_SEEDS}" ]; then
  echo "Will initialize Cassandra through ${CASSANDRA_SETUP_FILE}"
  cqlsh -f "${CASSANDRA_SETUP_FILE}"
fi
