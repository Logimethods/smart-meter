#!/bin/bash

set -e

source build.sh

extension="-local"

clear
echo "-----------------------------------------"
echo "build_dockerfile_inject $extension"
build_dockerfile_inject "$extension"

clear
echo "-----------------------------------------"
echo "build_dockerfile_app_streaming $extension"
build_dockerfile_app_streaming "$extension"

clear
echo "-----------------------------------------"
echo "build_dockerfile_app_batch $extension"
build_dockerfile_app_batch "$extension"

clear
echo "-----------------------------------------"
echo "build_dockerfile_prometheus $extension"
build_dockerfile_prometheus "$extension"

clear
echo "-----------------------------------------"
echo "build_dockerfile_monitor $extension"
build_dockerfile_monitor "$extension"

clear
echo "-----------------------------------------"
echo "build_dockerfile_cassandra $extension"
build_dockerfile_cassandra "$extension"

clear
echo "-----------------------------------------"
echo "build_dockerfile_telegraf $extension"
build_dockerfile_telegraf "$extension"

clear
echo "-----------------------------------------"
echo "build_dockerfile_cassandra_inject $extension"
build_dockerfile_cassandra_inject "$extension"
