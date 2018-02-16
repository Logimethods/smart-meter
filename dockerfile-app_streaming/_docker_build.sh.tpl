##!/bin/bash

docker pull ((docker-dz_spark-onbuild-repository)):((docker-dz_spark-onbuild-tag))((docker-additional-tag))
docker build -t ((docker-app_streaming-repository)):((docker-app_streaming-tag))((docker-additional-tag)) .