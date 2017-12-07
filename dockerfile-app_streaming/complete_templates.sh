#!/bin/bash

docker pull logimethods/smart-meter:compose-1.0-dev
docker run --rm -v `pwd`:/files logimethods/smart-meter:compose-1.0-dev complete_templates /files
chmod +x docker_build.sh
more ./docker_build.sh