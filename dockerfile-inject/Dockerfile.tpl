FROM maven:3-jdk-8-alpine as maven
COPY pom.xml .
RUN mvn package

# https://github.com/Logimethods/docker-eureka
FROM logimethods/eureka:entrypoint as entrypoint

FROM ${gatling_image}:${gatling_version}

COPY --from=entrypoint eureka_utils.sh /eureka_utils.sh
COPY --from=entrypoint entrypoint.sh /entrypoint.sh
COPY entrypoint_insert.sh /entrypoint_insert.sh
RUN apk --no-cache add jq bash netcat-openbsd>1.130
ENTRYPOINT ["/entrypoint.sh", "gatling.sh"]

COPY --from=maven target/app_inject-latest.jar /opt/gatling/lib/
COPY conf /opt/gatling/conf
COPY user-files /opt/gatling/user-files
