#!/bin/bash

if [ -f ${CASSANDRA_SETUP_FILE} ]; then
  cqlsh -f "${CASSANDRA_SETUP_FILE}"
fi
