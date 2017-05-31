/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.nats.connector.spark.app

import org.apache.log4j.{Level, LogManager, PropertyConfigurator}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator

object SparkPredictionAccuracy extends App with SparkPredictionProcessor {
  log.setLevel(Level.INFO)

  val (properties, targets, logLevel, sc, inputNatsStreaming, inputSubject, outputSubject, clusterId, outputNatsStreaming, natsUrl) = setup(args)
  
  val data = SparkPredictionProcessor.getData(sc, THRESHOLD)
  data.show()
  
  val splits = data.randomSplit(Array(0.6, 0.4), seed = 1234L)
  val train = splits(0)
  val test = splits(1)
  
  val model = trainer.fit(train)
  
  val result = model.transform(test)
  
  result.show()
  
  val predictionAndLabels = result.select("prediction", "label")
  val evaluator = new MulticlassClassificationEvaluator().setMetricName("accuracy")
  
  log.info("Size: " + data.count())
  log.info("Test Set Accuracy = " + evaluator.evaluate(predictionAndLabels))
}