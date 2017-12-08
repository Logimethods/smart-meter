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
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.storage.StorageLevel;
import io.nats.client.ConnectionFactory._
import java.nio.ByteBuffer

import org.apache.log4j.{Level, LogManager, PropertyConfigurator}

import com.logimethods.connector.nats.to_spark._
import com.logimethods.scala.connector.spark.to_nats._

import java.util.function._

import java.time._

object SparkAlertProcessor extends App with SparkStreamingProcessor {
  val log = LogManager.getRootLogger
  log.setLevel(Level.WARN)

  val (properties, target, logLevel, sc, ssc, inputNatsStreaming, inputSubject, outputSubject, clusterId, outputNatsStreaming, natsUrl, streamingDuration) =
    setupStreaming(args)

/*  def dataDecoder: Array[Byte] => Tuple2[Long,Float] = bytes => {
        val buffer = ByteBuffer.wrap(bytes);
        val epoch = buffer.getLong()
        val voltage = buffer.getFloat()
        (epoch, voltage)
      } */

  val messages =
    if (inputNatsStreaming) {
      NatsToSparkConnector
        .receiveFromNatsStreaming(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY, clusterId)
        .withNatsURL(natsUrl)
        .withSubjects(inputSubject)
        .withDataDecoder(dataDecoder)
        .asStreamOfKeyValue(ssc)
    } else {
      NatsToSparkConnector
        .receiveFromNats(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY)
        .withProperties(properties)
        .withSubjects(inputSubject)
        .withDataDecoder(dataDecoder)
        .asStreamOfKeyValue(ssc)
    }

  if (logLevel.equals("MESSAGES")) {
    messages.print()
  }

  // MAXIMUM values

  val max = messages.reduceByKey((t1, t2) => (Math.max(t1._1,t2._1), Math.max(t1._2,t2._2)))

  if (logLevel.equals("MAX")) {
    max.print()
  }

  def longFloatTupleEncoder: Tuple2[Long,Float] => Array[Byte] = tuple => {
        val buffer = ByteBuffer.allocate(4+8);
        buffer.putLong(tuple._1)
        buffer.putFloat(tuple._2)
        buffer.array()
      }

  if (outputNatsStreaming) {
    SparkToNatsConnectorPool.newStreamingPool(clusterId)
                            .withNatsURL(natsUrl)
                            .withSubjects(outputSubject)
                            .publishToNatsAsKeyValue(max, longFloatTupleEncoder)
  } else {
    SparkToNatsConnectorPool.newPool()
                            .withProperties(properties)
                            .withSubjects(outputSubject)
                            .publishToNatsAsKeyValue(max, longFloatTupleEncoder)
  }

/*  val maxReport = max.map(
      {case (subject, (epoch, voltage))
          => (subject, (LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN), voltage.toString())) })*/
  val maxReport = max.map(
      {case (subject, (epoch, voltage))
          => (subject, voltage.toString()) })
  SparkToNatsConnectorPool.newPool()
                          .withProperties(properties)
                          .withSubjects(outputSubject.replace("extract", "report"))
                          .publishToNatsAsKeyValue(maxReport)

  // ALERTS

  final val OFF_LIMIT_VOLTAGE_COUNT = 2
	final val OFF_LIMIT_VOLTAGE = 120f

  val offLimits = messages.filter( _._2._2 > OFF_LIMIT_VOLTAGE )
  if (logLevel.equals("OFF_LIMIT")) {
    offLimits.print()
  }

  val offLimitsCount = offLimits.mapValues(t => (t._1, 1))
                                .reduceByKey((t1, t2) => (Math.min(t1._1,t2._1), t1._2 + t2._2))
  if (logLevel.equals("OFF_LIMIT_COUNT")) {
    offLimitsCount.print()
  }

  val alerts = offLimitsCount.filter( _._2._2 >= OFF_LIMIT_VOLTAGE_COUNT )
  if (logLevel.equals("ALERTS")) {
    alerts.print()
  }

  def longIntTupleEncoder: Tuple2[Long,Int] => Array[Byte] = tuple => {
        val buffer = ByteBuffer.allocate(4+8);
        buffer.putLong(tuple._1)
        buffer.putInt(tuple._2)
        buffer.array()
      }

  val outputAlertSubject = outputSubject.replace("max", "alert")
  if (outputNatsStreaming) {
    SparkToNatsConnectorPool.newStreamingPool(clusterId)
                            .withNatsURL(natsUrl)
                            .withSubjects(outputAlertSubject)
                            .publishToNatsAsKeyValue(alerts, longIntTupleEncoder)
  } else {
    SparkToNatsConnectorPool.newPool()
                            .withProperties(properties)
                            .withSubjects(outputAlertSubject)
                            .publishToNatsAsKeyValue(alerts, longIntTupleEncoder)
  }

 /* val alertReport = alerts.map(
      {case (subject, (epoch, alert))
          => (subject, (LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN), alert.toString())) })*/
  val alertReport = alerts.map(
      {case (subject, (epoch, alert))
          => (subject, alert.toString()) })
  SparkToNatsConnectorPool.newPool()
                          .withProperties(properties)
                          .withSubjects(outputAlertSubject.replace("extract", "report"))
                          .publishToNatsAsKeyValue(alertReport)

  // Start
  ssc.start();

  ssc.awaitTermination()
}