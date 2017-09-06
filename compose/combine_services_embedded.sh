#!/bin/sh

## docker run logimethods/smart-meter:compose "_secrets" inject_metrics

SECRET_MODE="$1"
shift 1

source ./services_hierachy.sh

targets=$(echo "$@" | sed s/["^ "]*/'$'\&/g)

eval echo "$targets" \
  | xargs -n1 | sort -u | xargs \
  | sed s/["^ "]*/"## "\&/g

yamlreader \
  $( eval echo "$targets" \
  | xargs -n1 | sort -u | xargs \
  | sed s/["^ "]*/docker-compose-\&.yml/g )

