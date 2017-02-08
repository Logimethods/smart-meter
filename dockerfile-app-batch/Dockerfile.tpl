FROM ${spark_image}:${spark_version}-hadoop-${hadoop_version}

COPY ./libs/docker-smart-meter-app-batch-assembly.jar $SPARK_HOME

# CMD ["./bin/spark-submit", "--class", "com.logimethods.nats.connector.spark.app.SparkBatch", "--packages", "datastax:spark-cassandra-connector:${spark_cassandra_connector_version}", "docker-smart-meter-app-batch-assembly.jar"]
CMD ["./bin/spark-submit", "--class", "com.logimethods.nats.connector.spark.app.SparkBatch", "docker-smart-meter-app-batch-assembly.jar"]
