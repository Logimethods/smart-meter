##!/bin/bash

docker pull ((docker-dz_cassandra-repository)):((docker-dz_cassandra-tag))((docker-additional-tag))
docker build -t ((docker-app_cassandra-repository)):((docker-app_cassandra-tag))((docker-additional-tag)) .