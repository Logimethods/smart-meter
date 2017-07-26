#!/bin/bash

source build.sh

pushd ../docker-eureka
./build_exp.sh
popd

clear
echo "-----------------------------------------"
echo "build_dockerfile_nats_server $extension"
build_dockerfile_nats_server ""
docker push logimethods/smart-meter:nats-server
docker -H localhost:2374 pull logimethods/smart-meter:nats-server
