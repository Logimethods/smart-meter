docker exec -it $(docker ps | grep "cassandra-root" | rev | cut -d' ' -f1 | rev) cqlsh 
						# $(docker ps | grep "cassandra-root" | rev | cut -d' ' -f1 | rev)
