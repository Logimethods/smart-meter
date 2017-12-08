##!/bin/bash

#docker_mvn_spark_repository="logimethods/smart-meter"
#docker_mvn_spark-tag="mvn_spark"
# docker build -t ((docker-mvn_spark-repository)):((docker-mvn_spark-tag))((docker-additional-tag)) .
docker build -t logimethods/smart-meter:mvn_spark-onbuild-1.0-dev .