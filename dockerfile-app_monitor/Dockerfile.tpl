FROM maven:3-jdk-8-onbuild-alpine as mvn

# https://docs.docker.com/engine/userguide/eng-image/multistage-build/#use-multi-stage-builds
# https://github.com/Logimethods/docker-eureka
FROM logimethods/eureka:entrypoint as entrypoint

##- FROM frolvlad/alpine-scala:2.11
FROM ${scala_image}:${scala_main_version}

# https://stedolan.github.io/jq/
RUN apk --no-cache add \
  jq netcat-openbsd>1.130
  #bash

COPY --from=entrypoint eureka_utils.sh /eureka_utils.sh
COPY --from=entrypoint entrypoint.sh /entrypoint.sh

COPY entrypoint_insert.sh /entrypoint_insert.sh

COPY --from=mvn /usr/src/app/target/*.jar ./

# EXPOSE 5005 4040

ENTRYPOINT ["/entrypoint.sh", "scala", "-cp", "app_monitor-latest.jar"]
