#!/bin/bash

set -e

. set_properties_to_ek_templates.sh

clear
echo "-----------------------------------------"
echo "ek_telegraf"
pushd ek-telegraf
docker build -t logimethods/ek_telegraf .
popd

clear
echo "-----------------------------------------"
echo "ek_cassandra"
pushd ek-cassandra
docker build -t logimethods/ek_cassandra .
popd


clear
echo "-----------------------------------------"
echo "ek_nats-server"
pushd ek-nats-server
docker build -t logimethods/ek_nats-server .
popd

clear
echo "-----------------------------------------"
echo "ek_nats-client"
pushd ek-nats-client
go get github.com/nats-io/go-nats
env GOOS=linux GOARCH=amd64 go build main.go
file main
docker build -t logimethods/ek_nats-client .
popd
