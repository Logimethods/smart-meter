FROM maven:3-jdk-8-alpine

## https://github.com/carlossg/docker-maven#packaging-a-local-repository-with-the-image
COPY pom.xml /tmp/pom.xml
RUN mvn -B -f /tmp/pom.xml -s /usr/share/maven/ref/settings-docker.xml install
  # dependency:resolve

## https://github.com/carlossg/docker-maven/blob/8ab542b907e69c5269942bcc0915d8dffcc7e9fa/jdk-8/onbuild/Dockerfile
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

ONBUILD ADD . /usr/src/app
ONBUILD RUN mvn -s /usr/share/maven/ref/settings-docker.xml install
