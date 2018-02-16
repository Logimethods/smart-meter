/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.nats.connector.spark.app

import java.time.Instant

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

object SparkPredictionOracle extends App with SparkPredictionProcessor {
  log.setLevel(Level.WARN)

  val (properties, targets, logLevel, sc, inputNatsStreaming, inputSubject, outputSubject, clusterId, outputNatsStreaming, natsUrl) = setup(args)

  val streamingDuration = scala.util.Properties.envOrElse("STREAMING_DURATION", "2000").toInt
  val refreshInterval = 4 * streamingDuration
  println("STREAMING_DURATION = " + streamingDuration)

  import java.util.Properties

  var deadline = Instant.now.plusMillis(refreshInterval)

/*  // @See https://github.com/tyagihas/scala_nats
  import import io.nats.client._
  val opts : Properties = new Properties
  opts.put("servers", natsUrl);
  opts.put("queue", "smartmeter_oracle");
  val conn = Conn.connect(opts)
  conn.subscribe(inputSubject,
      (msg:MsgB)  => {
        if (Instant.now.isAfter(deadline)) {
          deadline = Instant.now.plusMillis(refreshInterval)
          Oracle.reset(sc)
        }
        val buffer = ByteBuffer.wrap(msg.body);
        val epoch = buffer.getLong()
        val temperature = buffer.getFloat()

//        val date = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN)
//        log.trace("Received a message on [" + msg.subject + "] : " + date + " / " + temperature)
        prediction(sc, epoch: Long, temperature: Float) match {
          case Some(true) =>
            val message = predictionMessage(epoch, temperature)
            conn.publish(outputSubject, message)
          case _ =>
        }
      })*/

  // @See https://github.com/nats-io/java-nats/blob/0.7.3/README.md
  import io.nats.client._

  val connectionFactory = new ConnectionFactory(natsUrl)
  val conn = connectionFactory.createConnection()

  conn.subscribe(inputSubject, "smartmeter_oracle", new MessageHandler {
	    override def onMessage(msg: Message) {
        if (Instant.now.isAfter(deadline)) {
          deadline = Instant.now.plusMillis(refreshInterval)
          Oracle.reset(sc)
        }
        val buffer = ByteBuffer.wrap(msg.getData())
        val epoch = buffer.getLong()
        val temperature = buffer.getFloat()

//        val date = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN)
//        log.trace("Received a message on [" + msg.subject + "] : " + date + " / " + temperature)
        prediction(sc, epoch: Long, temperature: Float) match {
          case Some(true) =>
            val message = predictionMessage(epoch, temperature)
            if (logLevel.contains("PREDICTION")) {
              println(message)
            }
            conn.publish(outputSubject, message.getBytes)
          case _ =>
        }
	    }})


  def prediction(sc: SparkContext, epoch: Long, temperature: Float): Option[Boolean] = {
    try {
  		val localModel = Oracle.getInstance(sc).value
  		if (localModel == null) {
  		  log.warn("No Prediciton Model Available")
  		  return None
  		}

      val date = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN)
  		val (hour, hourSin, hourCos, dayOfWeek) = SparkPredictionProcessor.extractDateComponents(date)
  		val values = List((hour, hourSin, hourCos, dayOfWeek, temperature))

    // @See https://spark.apache.org/docs/2.1.0/api/java/org/apache/spark/sql/SQLContext.implicits$.html
      val sqlContext = SQLContextSingleton.getInstance(sc)
      import sqlContext.implicits._

  		val dataFrame = values.toDF("hour", "hourSin", "hourCos", "dayOfWeek", "temperature")
  		val entry = SparkPredictionProcessor.assembler.transform(dataFrame)

  		Some(localModel.transform(entry).first.getDouble(6) > 0)
    } catch {
      case e: Throwable => if (log.isDebugEnabled()) e.printStackTrace
                           return None
    }
  }

  def predictionMessage(epoch: Long, temperature: Float) = {
    val timestamp = epoch * 1000
    s"""{"timestamp":$timestamp,"temperature":$temperature,"alert": $THRESHOLD}"""
  }

  // @See https://spark.apache.org/docs/2.1.0/streaming-programming-guide.html#accumulators-broadcast-variables-and-checkpoints
  import org.apache.spark.broadcast.Broadcast
  object Oracle {
    import java.lang.Runnable

    @volatile private var instance: Broadcast[MultilayerPerceptronClassificationModel] = null

    def getInstance(sc: SparkContext): Broadcast[MultilayerPerceptronClassificationModel] = {
      if (instance == null) {
        synchronized {
          if (instance == null) {
            val oracle = MultilayerPerceptronClassificationModel.load(PREDICTION_MODEL_PATH)
            println("MultilayerPerceptronClassificationModel loaded: " + oracle.uid)
            instance = sc.broadcast(oracle)
          }
        }
      }
      instance
    }

    def reset(sc: SparkContext) {
      new Thread(new Runnable {
        def run() {
          synchronized {
            try {
              val oracle = MultilayerPerceptronClassificationModel.load(PREDICTION_MODEL_PATH)
              println("MultilayerPerceptronClassificationModel loaded: " + oracle.uid)
              instance = sc.broadcast(oracle)
            } catch {
              case e: Throwable => if (log.isDebugEnabled()) e.printStackTrace
            }
          }
        }
      })start()
    }
}

}