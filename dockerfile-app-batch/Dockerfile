FROM gettyimages/spark:2.0.2-hadoop-2.7

COPY ./libs/docker-smart-meter-app-batch-assembly.jar $SPARK_HOME

CMD ["./bin/spark-submit", "--class", "com.logimethods.nats.connector.spark.app.SparkBatch", "--packages", "datastax:spark-cassandra-connector:2.0.0-M2-s_2.11", "docker-smart-meter-app-batch-assembly.jar"]
