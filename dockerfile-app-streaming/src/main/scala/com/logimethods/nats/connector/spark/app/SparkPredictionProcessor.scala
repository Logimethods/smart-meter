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

object SparkPredictionProcessor extends App with SparkProcessor {
  val log = LogManager.getRootLogger
  log.setLevel(Level.WARN)
  
  val (properties, logLevel, sc, ssc, inputStreaming, inputSubject, outputSubject, clusterId, outputStreaming, natsUrl) = setup(args)
  
  val stream = ssc.fileStream("/spark/storage/obj/")
  stream.print()
  
  val maxstats = sc.objectFile("/spark/storage/obj/maxstats")
  print(maxstats.collect())
  
//  val array = Array((6,Seq(117.03012, 117.420746)), (12,Seq(114.42395, 114.34491)), (18,Seq(114.547516, 115.31812, 114.90916)), (0,Seq(116.09781, 116.275246, 115.719284, 116.14771)), (7,Seq(116.681915, 116.28426)), (13,Seq(114.14914, 114.233665)), (19,Seq(115.70325, 115.77993, 115.60839, 115.81848)), (1,Seq(116.07925, 116.01583, 116.19858, 115.807526)), (8,Seq(115.42415, 116.038414)), (14,Seq(112.85625, 113.64558)), (20,Seq(116.296265, 116.89756, 116.99611, 116.26865)), (2,Seq(116.31506, 115.7546, 115.648834, 116.238464)), (9,Seq(115.79024, 115.468636)))
//  val rdd = sc.parallelize(array)
//  rdd.flatMap({case (i, s) => s.map { f => Row(i, (1, Seq(1), f)) }}).collect
//  rdd.foreach((i, s) => i)
 
  // Array([1.0,(4,[0,1,2,3],[-0.222222,0.5,-0.762712,-0.833333])], 
  
  // Start
//  ssc.start();		
  
//  ssc.awaitTermination()
}