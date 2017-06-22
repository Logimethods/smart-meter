#!/bin/bash

source build.sh

build_dockerfile_inject "local"

build_dockerfile_app_streaming "local"

build_dockerfile_app_batch "local"

build_dockerfile_prometheus "local"

build_dockerfile_monitor "local"

build_dockerfile_cassandra "local"

build_dockerfile_telegraf "local"

build_dockerfile_cassandra_inject "local"

build_dockerfile_nats_server "local"
