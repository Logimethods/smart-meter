#!/bin/bash

counts=$(curl --silent "$CASSANDRA_MAIN_URL:$CASSANDRA_COUNT_PORT" | head -n -2 | tail -n1)
json="{"
while IFS="|" read slot count; do
  clean_slot="$(echo -e "${slot}" | tr -d '[:space:]')"
  json="${json} \"${clean_slot}\":${count},"
done < <( echo "$counts")
json="${json::-1} }"
echo "${json}"
