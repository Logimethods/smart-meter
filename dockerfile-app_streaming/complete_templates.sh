#!/bin/bash

docker pull logimethods/smart-meter:inject-1.0-dev
docker run --rm -v `pwd`:/files logimethods/smart-meter:inject-1.0-dev complete_templates /files