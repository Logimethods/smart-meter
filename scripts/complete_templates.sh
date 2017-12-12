#!/bin/bash

# docker pull logimethods/smart-meter:app_compose-1.0-dev
docker run --rm -v "$@":/files logimethods/smart-meter:app_compose-1.0-dev complete_templates /files
