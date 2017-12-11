##!/bin/bash

location=`pwd`/../"$@"
echo "$location"

./complete_templates.sh "$location"
pushd "$location"
chmod +x _docker_build.sh
./_docker_build.sh
popd