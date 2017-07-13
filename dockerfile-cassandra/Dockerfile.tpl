## MIT License
##
## Copyright (c) 2016-2017 Logimethods
##
## Permission is hereby granted, free of charge, to any person obtaining a copy
## of this software and associated documentation files (the "Software"), to deal
## in the Software without restriction, including without limitation the rights
## to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
## copies of the Software, and to permit persons to whom the Software is
## furnished to do so, subject to the following conditions:
##
## The above copyright notice and this permission notice shall be included in all
## copies or substantial portions of the Software.
##
## THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
## IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
## FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
## AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
## LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
## OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
## SOFTWARE.

# https://docs.docker.com/engine/userguide/eng-image/multistage-build/#use-multi-stage-builds
# https://github.com/Logimethods/docker-eureka
#FROM logimethods/eureka:entrypoint as entrypoint
FROM entrypoint_exp as entrypoint

### MAIN FROM ###

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
    chmod 400 /etc/cassandra/jmxremote.password && \
    echo "cassandra readwrite" >> /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/management/jmxremote.access

### EUREKA ###

RUN apt-get update && apt-get install -y --no-install-recommends jq curl netcat-openbsd && rm -rf /var/lib/apt/lists/*

EXPOSE 6161

COPY --from=entrypoint eureka_utils.sh /eureka_utils.sh
COPY entrypoint_finalize.sh /entrypoint_finalize.sh
COPY merged_entrypoint.sh /merged_entrypoint.sh
RUN chmod +x /merged_entrypoint.sh
ENTRYPOINT ["/merged_entrypoint.sh"]

ENV READY_WHEN="Created default superuser role"

## !!! ENTRYPOINT provides amnesia about CMD !!!
CMD ["cassandra", "-f"]
