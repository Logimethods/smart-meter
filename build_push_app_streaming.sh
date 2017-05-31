#!/bin/bash

docker -H localhost:2374 service rm prediction_oracle prediction_trainer
pushd dockerfile-app-streaming
sbt test docker
popd
pushd dockerfile-app-streaming/target/docker
docker build -t logimethods/smart-meter:app-streaming .
docker push logimethods/smart-meter:app-streaming
popd
docker -H localhost:2374 pull logimethods/smart-meter:app-streaming

docker  -H localhost:2374  service create     --name prediction_trainer     -e NATS_URI=nats://smartmeter:xyz1234@nats:4222     -e SPARK_MASTER_URL=spark://spark-master:7077     -e HDFS_URL=hdfs://hadoop:9000     -e CASSANDRA_URL=cassandra_cluster_main     -e STREAMING_DURATION=5000     -e LOG_LEVEL=INFO     -e SPARK_CORES_MAX=2     -e ALERT_THRESHOLD=116     --network smartmeter     --constraint=node.role==manager     logimethods/smart-meter:app-streaming  "com.logimethods.nats.connector.spark.app.SparkPredictionTrainer"       "smartmeter.voltage.raw.forecast.12" "smartmeter.voltage.extract.prediction.12"       "Smartmeter PREDICTION TRAINER"

sleep 7
docker  -H localhost:2374 ps
