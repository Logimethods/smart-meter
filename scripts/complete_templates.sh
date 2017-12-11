#!/bin/bash

docker pull logimethods/smart-meter:compose-1.0-dev
docker run --rm -v "$@":/files logimethods/smart-meter:compose-1.0-dev complete_templates /files
