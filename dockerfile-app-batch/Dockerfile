# GENERATED FILE, please do not modify nor store into Git #
FROM gettyimages/spark:2.1.1-hadoop-2.7

COPY ./libs/docker-smart-meter-app-batch-assembly.jar $SPARK_HOME

# CMD ["./bin/spark-submit", "--class", "com.logimethods.nats.connector.spark.app.SparkBatch", "--packages", "datastax:spark-cassandra-connector:2.0.3", "docker-smart-meter-app-batch-assembly.jar"]
CMD ["./bin/spark-submit", "--class", "com.logimethods.nats.connector.spark.app.SparkBatch", "docker-smart-meter-app-batch-assembly.jar"]
