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
import com.datastax.spark.connector.streaming._
import com.datastax.spark.connector.SomeColumns

//import io.nats.client.Constants._
import io.nats.client.ConnectionFactory._
import java.nio.ByteBuffer

import org.apache.log4j.{Level, LogManager, PropertyConfigurator}

import com.logimethods.connector.nats.to_spark._
import com.logimethods.scala.connector.spark.to_nats._

import java.util.function._

import java.time.{LocalDateTime, ZoneOffset}

object SparkMaxProcessor extends App with SparkStreamingProcessor {
  val log = LogManager.getRootLogger
  log.setLevel(Level.WARN)
  
  val (properties, target, logLevel, sc, ssc, inputNatsStreaming, inputSubject, outputSubject, clusterId, outputNatsStreaming, natsUrl) = setupStreaming(args)
    
  def dataDecoder: Array[Byte] => Tuple2[Long,Float] = bytes => {
        val buffer = ByteBuffer.wrap(bytes);
        val epoch = buffer.getLong()
        val value = buffer.getFloat()
        (epoch, value)  
      }
  
  // MAX Voltages by Epoch //
  
  if ((target == "ALL") || (target.contains("MAX"))) {
    val inputDataSubject = inputSubject + ".data.>"
    
    val messages =
      if (inputNatsStreaming) {
        NatsToSparkConnector
          .receiveFromNatsStreaming(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY, clusterId)
          .withNatsURL(natsUrl)
          .withSubjects(inputDataSubject)
          .withDataDecoder(dataDecoder)
          .asStreamOf(ssc)
      } else {
        NatsToSparkConnector
          .receiveFromNats(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY)
          .withProperties(properties)
          .withSubjects(inputDataSubject)
          .withDataDecoder(dataDecoder)
          .asStreamOf(ssc)
      }
  
    if (logLevel.contains("MESSAGES")) {
      messages.print()
    }
    
    // To Cassandra
    
    val maxByEpoch = messages.reduceByKey(Math.max(_,_))
    maxByEpoch.saveToCassandra("smartmeter", "max_voltage")
  
    if (logLevel.contains("MAX")) {
      maxByEpoch.print()
    }
    
    // To NATS 
    
    val maxReport = maxByEpoch.map({case (epoch, voltage) => (s"""{"epoch": $epoch, "voltage": $voltage}""") })
            
    SparkToNatsConnectorPool.newPool()
                            .withProperties(properties)
                            .withSubjects(outputSubject)
                            .publishToNats(maxReport)
  
               
    if (logLevel.contains("MAX_REPORT")) {
      maxReport.print()
    }
  }
  
  // Temperature //

  if ((target == "ALL") || (target.contains("TEMPERATURE"))) {  
    val inputTemperatureSubject = inputSubject + ".temperature"
    
    val temperatures =
      if (inputNatsStreaming) {
        NatsToSparkConnector
          .receiveFromNatsStreaming(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY, clusterId)
          .withNatsURL(natsUrl)
          .withSubjects(inputTemperatureSubject)
          .withDataDecoder(dataDecoder)
          .asStreamOf(ssc)
      } else {
        NatsToSparkConnector
          .receiveFromNats(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY)
          .withProperties(properties)
          .withSubjects(inputTemperatureSubject)
          .withDataDecoder(dataDecoder)
          .asStreamOf(ssc)
      }
  
    // Ideally, should be the AVG
    val singleTemperature = temperatures.reduceByKey(Math.max(_,_))
    
    if (logLevel.contains("TEMPERATURE")) {
      singleTemperature.print()
    }
  
    singleTemperature.saveToCassandra("smartmeter", "temperature")
  }
  
  // Start //
  ssc.start();		
  
  ssc.awaitTermination()
}