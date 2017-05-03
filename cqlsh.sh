#!/bin/bash

. configuration.properties

# docker exec -it $(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) cqlsh
						# $(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev)

docker run -it --rm --net=smartmeter logimethods/smart-meter:cassandra cqlsh "${CASSANDRA_MAIN_NAME}"
