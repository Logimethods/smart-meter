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
import io.nats.client.Constants._

import org.apache.log4j.{Level, LogManager, PropertyConfigurator}

import com.logimethods.connector.nats.to_spark._
import com.logimethods.scala.connector.spark.to_nats._

import java.util.function._
// https://github.com/scala/scala-java8-compat
import scala.compat.java8.FunctionConverters._

object SparkProcessor extends App {
  val log = LogManager.getRootLogger
  log.setLevel(Level.WARN)
  
  Thread.sleep(5000)

  val inputSubject = args(0)
  val inputStreaming = inputSubject.toUpperCase.contains("STREAMING")
  val outputSubject = args(1)
  val outputStreaming = outputSubject.toUpperCase.contains("STREAMING")
  println("Will process messages from " + inputSubject + " to " + outputSubject)
  
  val logLevel = scala.util.Properties.envOrElse("LOG_LEVEL", "INFO")
  println("LOG_LEVEL = " + logLevel)

  val sparkMasterUrl = System.getenv("SPARK_MASTER_URL")
  println("SPARK_MASTER_URL = " + sparkMasterUrl)
  val conf = new SparkConf().setAppName("NATS Data Processing").setMaster(sparkMasterUrl);
  val sc = new SparkContext(conf);
//  val jarFilesRegex = "java-nats-streaming-(.*)jar|guava(.*)jar|protobuf-java(.*)jar|jnats-(.*)jar|nats-connector-spark-(.*)jar|docker-nats-connector-spark(.*)jar"
  val jarFilesRegex = "(.*)jar"
  for (file <- new File("/app/").listFiles.filter(_.getName.matches(jarFilesRegex))) 
    { sc.addJar(file.getAbsolutePath) }
  val ssc = new StreamingContext(sc, new Duration(2000));

  val properties = new Properties();
  val natsUrl = System.getenv("NATS_URI")
  println("NATS_URI = " + natsUrl)
  properties.put("servers", natsUrl)
  properties.put(PROP_URL, natsUrl)
  
  val clusterId = System.getenv("NATS_CLUSTER_ID")

  object dataDecoderX extends Serializable {    
    final val decoder = new java.util.function.Function[Array[Byte],Tuple2[Long,Float]] {
      override def apply(bytes: Array[Byte]):Tuple2[Long,Float] = {
        import java.nio.ByteBuffer
        val buffer = ByteBuffer.wrap(bytes);
        val epoch = buffer.getLong()
        val voltage = buffer.getFloat()
        (epoch, voltage)  
      }
    }
  }  
  
  val dataDecoder: Array[Byte] => Tuple2[Long,Float] = bytes => {
        import java.nio.ByteBuffer
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
        .withDataDecoder(dataDecoder.asJava)
        .asStreamOfKeyValue(ssc)
    } else {
      NatsToSparkConnector
        .receiveFromNats(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY)
        .withProperties(properties)
        .withSubjects(inputSubject)
        .withDataDecoder(dataDecoder.asJava)
        .asStreamOfKeyValue(ssc)
    }

  if (logLevel.equals("MESSAGES")) {
    messages.print()
  }
  
  // MAXIMUM values
  
  val floats = messages.map(t => (t._1, t._2._2))
  val max = floats.reduceByKey(Math.max(_,_))

  if (logLevel.equals("MAX")) {
    max.print()
  }

  if (outputStreaming) {
    SparkToNatsConnectorPool.newStreamingPool(clusterId)
                            .withNatsURL(natsUrl)
                            .withSubjects(outputSubject)
                            .publishToNatsAsKeyValue(max)
  } else {
    SparkToNatsConnectorPool.newPool()
                            .withProperties(properties)
                            .withSubjects(outputSubject)
                            .publishToNatsAsKeyValue(max)
  }
  
  // ALERTS
  
  final val OFF_LIMIT_VOLTAGE_COUNT = 2
	final val OFF_LIMIT_VOLTAGE = 120f
	
  val offLimits = floats.filter( _._2 > OFF_LIMIT_VOLTAGE )
  if (logLevel.equals("OFF_LIMIT")) { 
    offLimits.print()
  }

  val offLimitsCount = offLimits.mapValues(_ => 1).reduceByKey(_+_)
  if (logLevel.equals("OFF_LIMIT_COUNT")) { 
    offLimitsCount.print()
  }
  
  val alerts = offLimitsCount.filter( _._2 >= OFF_LIMIT_VOLTAGE_COUNT ) 
  if (logLevel.equals("ALERTS")) { 
    alerts.print()
  }
  
  val outputAlertSubject = outputSubject.replace("max", "alert")
  if (outputStreaming) {
    SparkToNatsConnectorPool.newStreamingPool(clusterId)
                            .withNatsURL(natsUrl)
                            .withSubjects(outputAlertSubject)
                            .publishToNatsAsKeyValue(alerts)
  } else {
    SparkToNatsConnectorPool.newPool()
                            .withProperties(properties)
                            .withSubjects(outputAlertSubject)
                            .publishToNatsAsKeyValue(alerts)
  }  

  // Start
  ssc.start();		
  
  ssc.awaitTermination()
}