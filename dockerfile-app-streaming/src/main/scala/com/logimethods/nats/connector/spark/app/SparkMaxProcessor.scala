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

object SparkMaxProcessor extends App with SparkProcessor {
  val log = LogManager.getRootLogger
  log.setLevel(Level.WARN)
  
//  val rawInputSubject = args(1)
//  args(1) += ".data.>"
  val (properties, logLevel, sc, ssc, inputStreaming, inputSubject, outputSubject, clusterId, outputStreaming, natsUrl) = setup(args)
  
  def dataDecoder: Array[Byte] => Tuple2[Long,Float] = bytes => {
        val buffer = ByteBuffer.wrap(bytes);
        val epoch = buffer.getLong()
        val voltage = buffer.getFloat()
        (epoch, voltage)  
      }
  
  val messages =
    if (inputStreaming) {
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
  
  // TEMPERATURES

  val temperatures = messages.filter({case (s, v) => s.endsWith("temperature")})
  temperatures.print()

  // MAXIMUM values
  
  val voltages = messages.filter({case (s, v) => s.startsWith("smartmeter.voltage.raw.data")})  
  val max = voltages.reduceByKey((t1, t2) => (Math.max(t1._1,t2._1), Math.max(t1._2,t2._2)))

  if (logLevel.equals("MAX")) {
    max.print()
  }
    
/*  def longFloatTupleEncoder: Tuple2[Long,Float] => Array[Byte] = tuple => {        
        val buffer = ByteBuffer.allocate(4+8);
        buffer.putLong(tuple._1)
        buffer.putFloat(tuple._2)        
        buffer.array()    
      }

  if (outputStreaming) {
    SparkToNatsConnectorPool.newStreamingPool(clusterId)
                            .withNatsURL(natsUrl)
                            .withSubjects(outputSubject)
                            .publishToNatsAsKeyValue(max, longFloatTupleEncoder)
  } else {
    SparkToNatsConnectorPool.newPool()
                            .withProperties(properties)
                            .withSubjects(outputSubject)
                            .publishToNatsAsKeyValue(max, longFloatTupleEncoder)
  }*/
  
  val maxReport = max.map(
      {case (subject, (epoch, voltage)) 
          => (subject, voltage.toString()) })
  SparkToNatsConnectorPool.newPool()
                          .withProperties(properties)
                          .withSubjects(outputSubject.replace("extract", "report"))
                          .publishToNatsAsKeyValue(maxReport)
  // maxReport.print()   
                            
  val maxDelta = max.map(
      {case (subject, (epoch, voltage)) 
          => (/*LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN).getDayOfWeek, */
                        LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN).getHour, 
                        voltage) })
  //maxDelta.print()
  
  val maxOfMax = maxDelta.reduceByKey(Math.max(_, _))
  
  maxOfMax.print()
  
  maxOfMax.saveToCassandra("smartmeter", "max_voltage_by_hour", SomeColumns("hour", "voltage_max") /*, writeConf*/)
  
  // Start
  ssc.start();		
  
  ssc.awaitTermination()
}