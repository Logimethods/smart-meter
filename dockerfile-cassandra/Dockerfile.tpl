# https://hub.docker.com/_/cassandra/
FROM cassandra:${cassandra_version}

COPY /cql /cql
