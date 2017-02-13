# docker exec -it $(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) cqlsh
						# $(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev)

docker run -it --rm --net=smart-meter-net logimethods/smart-meter:cassandra sh -c 'exec cqlsh "cassandra-1"'
