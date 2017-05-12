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

//import io.nats.client.Constants._
import io.nats.client.ConnectionFactory._
import java.nio.ByteBuffer

import org.apache.log4j.{Level, LogManager, PropertyConfigurator}

import com.logimethods.connector.nats.to_spark._
import com.logimethods.scala.connector.spark.to_nats._

import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator

import java.util.function._

import java.time.{LocalDateTime, ZoneOffset}

object SparkPredictionProcessor extends App with SparkStreamingProcessor {
  val log = LogManager.getRootLogger
  log.setLevel(Level.WARN)
  
  val (properties, target, logLevel, sc, ssc, inputNatsStreaming, inputSubject, outputSubject, clusterId, outputNatsStreaming, natsUrl) = setupStreaming(args)

  val THRESHOLD = System.getenv("ALERT_THRESHOLD").toFloat
  println("ALERT_THRESHOLD = " + THRESHOLD)
  
  val sqlContext= new org.apache.spark.sql.SQLContext(sc)
  import sqlContext.implicits._

  import com.datastax.spark.connector._
  
  // http://stackoverflow.com/questions/37513667/how-to-create-a-spark-dataset-from-an-rdd
  // https://github.com/apache/spark/blob/master/examples/src/main/scala/org/apache/spark/examples/ml/MultilayerPerceptronClassifierExample.scala
  val layers = Array[Int](4, 12, 12, 2)
  
  val trainer = new MultilayerPerceptronClassifier().setLayers(layers).setBlockSize(128).setSeed(1234L).setMaxIter(120)
  
  // http://stackoverflow.com/questions/33844591/prepare-data-for-multilayerperceptronclassifier-in-scala
  import java.time.{LocalDateTime, ZoneOffset}
  import scala.math._
  import org.apache.spark.ml.feature.VectorAssembler  

  implicit def bool2int(b:Boolean) = if (b) 1 else 0

  val assembler = new VectorAssembler()
    .setInputCols(Array("hourSin", "hourCos", "dayOfWeek", "temperature"))
    .setOutputCol("features")
  
  // Initial training
  var model = trainer.fit(getData())
  
  new Thread(new Runnable {
              def run() {
                 while( true ){
                   try {
                     model = trainer.fit(getData())
                     println("New model trained")
                   } catch {
                     case e: Throwable => log.error(e)
                   }
                 }
              }
             }).start()
  
  if (logLevel != "DEBUG") {
    println("Start Predictions")
    
    val forecasts =
      if (inputNatsStreaming) {
        NatsToSparkConnector
          .receiveFromNatsStreaming(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY, clusterId)
          .withNatsURL(natsUrl)
          .withSubjects(inputSubject)
          .withDataDecoder(dataDecoder)
          .asStreamOf(ssc)
      } else {
        NatsToSparkConnector
          .receiveFromNats(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY)
          .withProperties(properties)
          .withSubjects(inputSubject)
          .withDataDecoder(dataDecoder)
          .asStreamOf(ssc)
      }
  
    if (logLevel.contains("FORECASTS")) {
      forecasts.print()
    }

    val predictions = forecasts.map({case (epoch: Long, temperature: Float) => (epoch, temperature, predictionFunc(epoch,temperature)) })
    
    if (logLevel.contains("PREDICTIONS")) {
      predictions.print()
    }  
    
    val alerts = predictions.filter(_._3).map({case (epoch, temperature, flag) => predictionMessage(epoch, temperature) })

    SparkToNatsConnectorPool.newPool()
                            .withProperties(properties)
                            .withSubjects(outputSubject)
                            .publishToNats(alerts)
    
    // Start //
    ssc.start();		   
    ssc.awaitTermination()
    
  } else {    
    val data = getData()
    data.show()
    
    val splits = data.randomSplit(Array(0.6, 0.4), seed = 1234L)
    val train = splits(0)
    val test = splits(1)
    
    val model = trainer.fit(train)
    
    val result = model.transform(test)
    
    result.show()
    
    val predictionAndLabels = result.select("prediction", "label")
    val evaluator = new MulticlassClassificationEvaluator().setMetricName("accuracy")
    
    println("Size: " + data.count())
    println("Test Set Accuracy = " + evaluator.evaluate(predictionAndLabels))
  }
  
  def extractDateComponents(date: LocalDateTime) = {
    val hour = date.getHour
    val hourAngle = (hour.toFloat / 24) * 2 * Pi
    val hourSin = 50 * sin(hourAngle)
    val hourCos = 50 * cos(hourAngle)
    val dayOfWeek = (date.getDayOfWeek.ordinal / 4) * 50 // Mon to Friday -> 0, Sat & Sun -> 50
   
    (hour, hourSin, hourCos, dayOfWeek)
  }

  def dataDecoder: Array[Byte] => Tuple2[Long,Float] = bytes => {
        val buffer = ByteBuffer.wrap(bytes);
        val epoch = buffer.getLong()
        val value = buffer.getFloat()
        (epoch, value)  
      }
  
  def getData() = {
    val max_voltage = sc.cassandraTable("smartmeter", "max_voltage")
    val table = max_voltage.joinWithCassandraTable("smartmeter", "temperature")

    val flatten = table.map({case (v,t) =>
      val date = LocalDateTime.ofEpochSecond(v.get[Long]("epoch"), 0, ZoneOffset.MIN)
      val voltage = v.get[Float]("voltage")
      val label = (voltage > THRESHOLD):Int
      val temperature = t.get[Float]("temperature")
      // https://www.reddit.com/r/MachineLearning/comments/2hzuj5/how_do_i_encode_day_of_the_week_as_a_predictor/
      val (hour, hourSin, hourCos, dayOfWeek) = extractDateComponents(date)
      
      (label, voltage, hour, hourSin, hourCos, dayOfWeek, temperature)})
    
    val dataframes = flatten.toDF("label", "voltage", "hour", "hourSin", "hourCos", "dayOfWeek", "temperature")
  
    assembler.transform(dataframes)
  }
  
  def predictionFunc(epoch: Long, temperature: Float) = {
          val date = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN)
//println("Received a message on [" + msg.subject + "] : " + date + " / " + temperature)
          
          val (hour, hourSin, hourCos, dayOfWeek) = extractDateComponents(date)
          val values = List((hour, hourSin, hourCos, dayOfWeek, temperature))
          val dataFrame = values.toDF("hour", "hourSin", "hourCos", "dayOfWeek", "temperature")
          val entry = assembler.transform(dataFrame)
                    
          model.transform(entry).first.getDouble(6) > 0    
  }
  
  def predictionMessage(epoch: Long, temperature: Float) = {
    val timestamp = epoch * 1000
    s"""{"timestamp":$timestamp,"temperature":$temperature,"alert": $THRESHOLD}"""
  }
}