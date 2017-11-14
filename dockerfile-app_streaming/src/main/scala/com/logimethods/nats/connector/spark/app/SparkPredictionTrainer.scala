/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.nats.connector.spark.app

import java.util.Properties;
import java.io.File
import java.io.Serializable

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming._

import io.nats.client.ConnectionFactory._
import java.nio.ByteBuffer

import org.apache.log4j.{Level, LogManager, PropertyConfigurator}

import com.logimethods.connector.nats.to_spark._
import com.logimethods.scala.connector.spark.to_nats._

import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator

import java.util.function._

import java.time.{LocalDateTime, ZoneOffset}
import java.time.DayOfWeek._

import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel

object SparkPredictionTrainer extends App with SparkPredictionProcessor {
  log.setLevel(Level.WARN)

  val (properties, targets, logLevel, sc, inputNatsStreaming, inputSubject, outputSubject, clusterId, outputNatsStreaming, natsUrl) = setup(args)
 
  val streamingDuration = scala.util.Properties.envOrElse("STREAMING_DURATION", "2000").toInt
  println("STREAMING_DURATION = " + streamingDuration)
  
  new Thread(new Runnable {
              def run() {
                 while( true ){
//                   try {
                     val data = SparkPredictionProcessor.getData(sc, THRESHOLD)
                     val model = trainer.fit(data)
                     model.write.overwrite.save(PREDICTION_MODEL_PATH)
                     println("New model of size " + data.count() + " trained: " + model.uid)
                     Thread.sleep(streamingDuration)
//                   } catch {
//                     case e: Throwable => log.error(e)
//                   }
                 }
              }
             }).start()
}