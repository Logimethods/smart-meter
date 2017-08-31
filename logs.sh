#!/bin/bash
echo "logs of $1"
while :
do
  line=$(docker ps | grep "$1")
  echo -n "$line" $'\r'
  id=$(echo $line | rev | cut -d' ' -f1 | rev)
  docker logs "$id" \
  && break || sleep 1
done