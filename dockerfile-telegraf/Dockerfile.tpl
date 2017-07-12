# https://docs.docker.com/engine/userguide/eng-image/multistage-build/#use-multi-stage-builds
# https://github.com/Logimethods/docker-eureka
#FROM logimethods/eureka:entrypoint as entrypoint
FROM entrypoint_exp as entrypoint

### MAIN FROM ###

FROM telegraf:${telegraf_version}

# https://github.com/iron-io/dockers/blob/master/java/java-1.8/Dockerfile
RUN echo '@edge http://nl.alpinelinux.org/alpine/edge/main' >> /etc/apk/repositories \
  && echo '@community http://nl.alpinelinux.org/alpine/edge/community' >> /etc/apk/repositories \
  && apk update \
  && apk upgrade \
  && apk add openjdk8-jre-base@community jq bash curl netcat-openbsd \
  && rm -rf /var/cache/apk/*

# http://dba.stackexchange.com/questions/68332/how-can-i-get-nodetool-without-cassandra
COPY tar/ /nodetool/
RUN cd /nodetool && \
    tar -zxf dsc-cassandra-3.0.9-bin.tar.gz &&\
    rm dsc-cassandra-3.0.9-bin.tar.gz

# RUN apk --no-cache add docker

VOLUME ["/etc/telegraf/"]

COPY --from=entrypoint eureka_utils.sh /eureka_utils.sh
COPY --from=entrypoint entrypoint.sh /entrypoint.sh
COPY entrypoint_insert.sh /entrypoint_insert.sh
ENTRYPOINT ["/entrypoint.sh"]

COPY script/ /etc/telegraf/
RUN chmod +x /etc/telegraf/*.sh
COPY conf/ /etc/telegraf/
