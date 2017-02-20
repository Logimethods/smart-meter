# https://hub.docker.com/_/cassandra/
FROM cassandra:${cassandra_version}

### JOLOKIA ###

# https://community.wavefront.com/docs/DOC-1210
# https://hub.docker.com/r/fourstacks/cassandra/~/dockerfile/
# ENV CASSANDRA_OPTIONS=-R

RUN mkdir -p /opt/jolokia/
COPY libs/jolokia-jvm-1.3.5-agent.jar /opt/jolokia/
RUN echo "JVM_OPTS=\"\\\$JVM_OPTS -javaagent:/opt/jolokia/jolokia-jvm-1.3.5-agent.jar=port=${JOLOKIA_PORT},host=*\"" >> /etc/cassandra/cassandra-env.sh

### CQL ###

COPY /cql /cql

### JMX ###

# https://support.datastax.com/hc/en-us/articles/204226179-Step-by-step-instructions-for-securing-JMX-authentication-for-nodetool-utility-OpsCenter-and-JConsole
COPY ./jmxremote.password /etc/cassandra/jmxremote.password
RUN chown cassandra:cassandra /etc/cassandra/jmxremote.password && \
    chmod 400 /etc/cassandra/jmxremote.password&& \
    echo "cassandra readwrite" >> /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/management/jmxremote.access
