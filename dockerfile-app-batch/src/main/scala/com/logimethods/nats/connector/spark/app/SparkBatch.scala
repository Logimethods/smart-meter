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
import org.apache.log4j.Logger

import org.apache.spark.sql.SparkSession

//import com.datastax.spark.connector._
//import com.datastax.spark.connector.cql.CassandraConnector

// @see http://stackoverflow.com/questions/39423131/how-to-use-cassandra-context-in-spark-2-0
// @see https://databricks.com/blog/2016/08/15/how-to-use-sparksession-in-apache-spark-2-0.html
// @see https://dzone.com/articles/cassandra-with-spark-20-building-rest-api
object SparkBatch extends App {
  val logLevel = System.getenv("APP_BATCH_LOG_LEVEL")
  println("APP_BATCH_LOG_LEVEL = " + logLevel)
  if ("DEBUG" != logLevel) {
  	Logger.getLogger("org").setLevel(Level.OFF)
  }
  
  val cassandraUrl = System.getenv("CASSANDRA_URL")
  println("CASSANDRA_URL = " + cassandraUrl)
  
  val sparkMasterUrl = System.getenv("SPARK_MASTER_URL")
  println("SPARK_MASTER_URL = " + sparkMasterUrl)
  
  val spark = SparkSession
    .builder()
    .master(sparkMasterUrl)
    .appName("Smartmeter Batch")
    .config("spark.cassandra.connection.host", cassandraUrl)
    //   .config("spark.sql.warehouse.dir", warehouseLocation)
    //.enableHiveSupport()
    .getOrCreate()
  
  spark
    .read
    .format("org.apache.spark.sql.cassandra")
    .options(Map("keyspace" -> "smartmeter", "table" -> "raw_voltage_data"))
    .load
    .createOrReplaceTempView("raw_voltage_data")
  
  val rawVoltageData = spark.sql("select * from raw_voltage_data")
  rawVoltageData.show(10)
  /**
    > ./run_spark_shell.sh
    scala> rawVoltageData.show(10)
    +----+-----------+----------+----+-----+---+----+------+-----------+----------+ 
    |line|transformer|usagepoint|year|month|day|hour|minute|day_of_week|   voltage|
    +----+-----------+----------+----+-----+---+----+------+-----------+----------+
    |   1|          1|         2|2016|   12| 24|   3|    16|          6| 118.69983|
    |   1|          1|         2|2016|   12| 24|   3|     1|          6| 118.20401|
    |   1|          1|         2|2016|   12| 24|   2|    46|          6|116.174644|
    |   1|          1|         2|2016|   12| 24|   2|    31|          6| 118.22744|
  
   **/
  
  val byTransformer = rawVoltageData.groupBy("line", "transformer", "year", "month", "day")
  val avgByTransformer = byTransformer.avg("voltage").withColumnRenamed("avg(voltage)", "voltage_avg")
  avgByTransformer.show(10)
  /**
    scala> byTransformer.avg("voltage").show()
    +----+-----------+----+-----+---+------------------+                            
    |line|transformer|year|month|day|      avg(voltage)|
    +----+-----------+----+-----+---+------------------+
    |   1|          2|2016|   12| 24|115.20123254685174|
    |   3|          2|2016|   12| 24|119.44731685093471|
    |   1|          2|2016|   12| 23|114.10264686916186|
    |   2|          3|2016|   12| 24|117.94807270595005|
  
   **/
  
  // @see http://stackoverflow.com/questions/40324153/what-is-the-best-way-to-insert-update-rows-in-cassandra-table-via-java-spark
  //Save data to Cassandra
  import org.apache.spark.sql.SaveMode
  avgByTransformer.write.format("org.apache.spark.sql.cassandra").options(Map("keyspace" -> "smartmeter", "table" -> "avg_voltage_by_transformer")).mode(SaveMode.Overwrite).save();
}
