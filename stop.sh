docker service rm $(docker service ls -q)
docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)
#https://gist.github.com/brianclements/f72b2de8e307c7b56689
docker rmi $(docker images | grep "<none>" | awk '{print $3}') 2>/dev/null || echo "No untagged images to delete."
#docker volume prune
docker volume rm $(docker volume ls -q | grep -v grafana-volume)
# | grep -v cassandra-volume
