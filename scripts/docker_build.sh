##!/bin/bash

location=`pwd`/../"$@"
echo "$location"

bash complete_templates.sh "$location"
pushd "$location"
bash _docker_build.sh
popd