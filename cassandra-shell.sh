docker exec -it spark-master.1.5kqriu5uxuru4m6gzewerds5k bash -c "spark-shell --packages datastax:spark-cassandra-connector:2.0.0-M2-s_2.11 --conf spark.cassandra.connection.host=cassandra-root"
