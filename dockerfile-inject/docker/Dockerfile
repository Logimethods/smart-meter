# https://docs.docker.com/engine/userguide/eng-image/multistage-build/#use-multi-stage-builds
# https://github.com/Logimethods/docker-eureka
FROM logimethods/eureka:entrypoint as entrypoint
#FROM entrypoint_exp as entrypoint

### MAIN FROM ###
FROM denvazh/gatling:2.2.2
ADD 0/scala-library-2.11.8.jar 1/nats-connector-gatling_2.11-0.4.0-SNAPSHOT.jar 2/java-nats-streaming-0.5.0.jar 3/breeze_2.11-0.12.jar 4/breeze-macros_2.11-0.12.jar 5/scala-reflect-2.11.8.jar 6/core-1.1.2.jar 7/arpack_combined_all-0.1.jar 8/opencsv-2.3.jar 9/jtransforms-2.4.0.jar 10/junit-4.8.2.jar 11/commons-math3-3.2.jar 12/spire_2.11-0.7.4.jar 13/spire-macros_2.11-0.7.4.jar 14/shapeless_2.11-2.0.0.jar 15/gatling-core-2.2.2.jar 16/gatling-commons-2.2.2.jar 17/config-1.3.0.jar 18/fastring_2.11-0.2.4.jar 19/boopickle_2.11-1.2.0.jar 20/quicklens_2.11-1.4.7.jar 21/scala-java8-compat_2.11-0.8.0-RC1.jar 22/scala-logging_2.11-3.4.0.jar 23/logback-classic-1.1.7.jar 24/logback-core-1.1.7.jar 25/akka-actor_2.11-2.4.7.jar 26/akka-slf4j_2.11-2.4.7.jar 27/jackson-dataformat-csv-2.7.5.jar 28/jackson-core-2.7.5.jar 29/jackson-databind-2.7.5.jar 30/jackson-annotations-2.7.0.jar 31/concurrentlinkedhashmap-lru-1.4.2.jar 32/scala-parser-combinators_2.11-1.0.4.jar 33/scopt_2.11-3.5.0.jar 34/jsonpath_2.11-0.6.7.jar 35/boon-json-0.6.2.jar 36/boon-reflekt-0.6.2.jar 37/slf4j-api-1.7.25.jar 38/Saxon-HE-9.7.0-5.jar 39/jodd-lagarto-3.7.1.jar 40/jodd-core-3.7.1.jar 41/jodd-log-3.7.1.jar 42/scalactic_2.11-3.0.0.jar ./lib/
COPY 43/docker-smart-meter-inject_2.11-0.4.0-SNAPSHOT.jar ./lib/docker-smart-meter-inject_2.11-0.4.0-SNAPSHOT.jar
ADD 44/user-files ./user-files
ADD 45/conf ./conf
COPY --from=entrypoint eureka_utils.sh /eureka_utils.sh
COPY --from=entrypoint entrypoint.sh /entrypoint.sh

COPY entrypoint_insert.sh /entrypoint_insert.sh

RUN apk --no-cache add jq bash netcat-openbsd>1.130

ENTRYPOINT ["/entrypoint.sh", "gatling.sh"]
