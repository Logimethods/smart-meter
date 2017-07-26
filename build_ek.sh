#!/bin/bash

set -e

. set_properties_to_ek_templates.sh

clear
echo "-----------------------------------------"
echo "ek_telegraf"
pushd ek-telegraf
docker build -t logimethods/ek_telegraf .
popd
