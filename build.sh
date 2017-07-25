#!/bin/bash

. set_properties_to_dockerfile_templates.sh

build_dockerfile_inject() {
  pushd dockerfile-inject
  #sbt update
  #sbt test docker
  #sbt eclipse
  sbt clean assembly dockerFileTask
  pushd target/docker
  mv Dockerfile Dockerfile_middle
  cp ../../entrypoint_insert.sh .
  cat ../../Dockerfile_pre Dockerfile_middle ../../Dockerfile_post >> Dockerfile
  docker build -t logimethods/smart-meter:inject$1 .
  popd
  popd
}

build_dockerfile_app_streaming() {
  pushd dockerfile-app-streaming
  #sbt update
  #sbt test docker
  #sbt eclipse
  sbt clean assembly dockerFileTask
  pushd target/docker
  mv Dockerfile Dockerfile_middle
  cp ../../entrypoint_insert.sh .
  cat ../../Dockerfile_pre Dockerfile_middle ../../Dockerfile_post >> Dockerfile
  docker build -t logimethods/smart-meter:app-streaming$1 .
  popd
  popd
}

build_dockerfile_app_batch() {
  pushd dockerfile-app-batch
  sbt update test assembly eclipse
  mkdir -p libs
  mv target/scala-*/*.jar libs/
  docker build -t logimethods/smart-meter:app-batch$1 .
  popd
}

build_dockerfile_prometheus() {
  pushd dockerfile-prometheus
  docker build -t logimethods/smart-meter:prometheus$1 .
  popd
}

build_dockerfile_monitor() {
  pushd dockerfile-monitor
  sbt update
  sbt test docker
  sbt eclipse
  popd
}

build_dockerfile_cassandra() {
  pushd dockerfile-cassandra
  docker build -t logimethods/smart-meter:cassandra$1 .
  popd
}

build_dockerfile_telegraf() {
  pushd dockerfile-telegraf
  docker build -t logimethods/smart-meter:telegraf$1 .
  popd
}

build_dockerfile_cassandra_inject() {
  pushd dockerfile-cassandra-inject
  docker build -t logimethods/smart-meter:cassandra-inject$1 .
  popd
}

build_dockerfile_nats_server() {
  pushd dockerfile-nats-server
  docker build -t logimethods/smart-meter:nats-server$1 .
  popd
}

build_dockerfile_nats_client() {
  pushd dockerfile-nats-client
  env GOOS=linux GOARCH=amd64 go build main.go
  file main
  docker build -t logimethods/smart-meter:nats-client$1 .
  popd
}
