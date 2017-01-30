docker exec -it $(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev) cqlsh 
						# $(docker ps | grep "cassandra" | rev | cut -d' ' -f1 | rev)
