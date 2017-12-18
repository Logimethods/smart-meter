##!/bin/bash

## docker pull prom/prometheus:${prometheus_version}
docker build -t ((docker-app_prometheus-repository)):((docker-app_prometheus-tag))((docker-additional-tag)) .