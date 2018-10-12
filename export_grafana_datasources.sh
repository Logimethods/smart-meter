#!/bin/sh

## See https://rmoff.net/2017/08/08/simple-export-import-of-data-sources-in-grafana/

URL=${1:-'http://localhost'}
DATA='dockerfile-app_metrics/data_sources/'

source properties/configuration.properties

mkdir -p "$DATA"
curl -s "${URL}/api/datasources"  -u "admin:${GF_SECURITY_ADMIN_PASSWORD}"|jq -c -M '.[]'|split -l 1 - "$DATA"