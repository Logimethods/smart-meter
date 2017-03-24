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

import java.util.function._

import java.time.{LocalDateTime, ZoneOffset}

trait SparkProcessor {
  def setup(args: Array[String]) = {
    val inputSubject = args(0)
    val inputStreaming = inputSubject.toUpperCase.contains("STREAMING")
    val outputSubject = args(1)
    val outputStreaming = outputSubject.toUpperCase.contains("STREAMING")
    println("Will process messages from " + inputSubject + " to " + outputSubject)
    
    val logLevel = scala.util.Properties.envOrElse("LOG_LEVEL", "INFO")
    println("LOG_LEVEL = " + logLevel)
  
    val sparkMasterUrl = System.getenv("SPARK_MASTER_URL")
    println("SPARK_MASTER_URL = " + sparkMasterUrl)
    val conf = new SparkConf().setAppName(args(2)).setMaster(sparkMasterUrl);
    val sc = new SparkContext(conf);
  //  val jarFilesRegex = "java-nats-streaming-(.*)jar|guava(.*)jar|protobuf-java(.*)jar|jnats-(.*)jar|nats-connector-spark-(.*)jar|docker-nats-connector-spark(.*)jar"
    val jarFilesRegex = "(.*)jar"
    for (file <- new File("/app/").listFiles.filter(_.getName.matches(jarFilesRegex))) 
      { sc.addJar(file.getAbsolutePath) }
    val streamingDuration = scala.util.Properties.envOrElse("STREAMING_DURATION", "2000").toInt
    val ssc = new StreamingContext(sc, new Duration(streamingDuration));
    ssc.checkpoint("/spark/storage")
  
    val properties = new Properties();
    val natsUrl = System.getenv("NATS_URI")
    println("NATS_URI = " + natsUrl)
    properties.put("servers", natsUrl)
    properties.put(PROP_URL, natsUrl)
  
    val clusterId = System.getenv("NATS_CLUSTER_ID")
    
    (properties, logLevel, ssc, inputStreaming, inputSubject, outputSubject, clusterId, outputStreaming, natsUrl)
  }
}