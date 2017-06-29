#!/bin/bash
# https://github.com/moby/moby/issues/26083"
while :
do
    for sid in $(docker service ps $1 |grep Ready | cut -d" " -f1); do
        cid=$(docker inspect --format "{{.Status.ContainerStatus.ContainerID}}" $sid)
        if [ $cid ];then
            docker logs $cid
        fi
    done
done
