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

import org.apache.log4j.{Level, LogManager, PropertyConfigurator}

import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector

// @see https://github.com/datastax/spark-cassandra-connector
object SparkBatch extends App {
  val cassandraUrl = System.getenv("CASSANDRA_URL")
  println("CASSANDRA_URL = " + cassandraUrl)
  val conf = new SparkConf(true)
          .set("spark.cassandra.connection.host", cassandraUrl)
//          .set("spark.cassandra.auth.username", "cassandra")            
//          .set("spark.cassandra.auth.password", "cassandra")
  
  val sparkMasterUrl = System.getenv("SPARK_MASTER_URL")
  println("SPARK_MASTER_URL = " + sparkMasterUrl)
  val sc = new SparkContext(sparkMasterUrl, "batch", conf)
  
  val connector = CassandraConnector(sc.getConf)
}