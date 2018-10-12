#!/bin/sh

## See https://rmoff.net/2017/08/08/simple-export-import-of-data-sources-in-grafana/

URL=${1:-'http://localhost'}
DATA='dockerfile-app_metrics/data_sources/'

source properties/configuration.properties

for i in "$DATA"/*; do \
    curl -X "POST" "${URL}/api/datasources" \
    -H "Content-Type: application/json" \
     --user "admin:${GF_SECURITY_ADMIN_PASSWORD}" \
     --data-binary @$i
done