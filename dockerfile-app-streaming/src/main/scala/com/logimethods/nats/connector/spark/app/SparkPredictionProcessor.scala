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
import java.time.DayOfWeek._

import com.datastax.spark.connector._

import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel

trait SparkPredictionProcessor extends SparkProcessor {
  val log = LogManager.getRootLogger

  val HDFS_URL = System.getenv("HDFS_URL")
  println("HDFS_URL = " + HDFS_URL)
  
  val PREDICTION_MODEL_PATH = HDFS_URL + "/smartmeter/voltage_prediction.model"

  val THRESHOLD = System.getenv("ALERT_THRESHOLD").toFloat
  println("ALERT_THRESHOLD = " + THRESHOLD)  
  
  // http://stackoverflow.com/questions/37513667/how-to-create-a-spark-dataset-from-an-rdd
  // https://github.com/apache/spark/blob/master/examples/src/main/scala/org/apache/spark/examples/ml/MultilayerPerceptronClassifierExample.scala
  val layers = Array[Int](4, 16, 12, 6, 2)
  
  val trainer = new MultilayerPerceptronClassifier().setLayers(layers).setBlockSize(128).setSeed(1234L).setMaxIter(120)
  
}

object SparkPredictionProcessor {
  // http://stackoverflow.com/questions/33844591/prepare-data-for-multilayerperceptronclassifier-in-scala
  import java.time.{LocalDateTime, ZoneOffset}
  import scala.math._
  import org.apache.spark.ml.feature.VectorAssembler  

  implicit def bool2int(b:Boolean) = if (b) 1 else 0

  val assembler = new VectorAssembler()
    .setInputCols(Array("hourSin", "hourCos", "dayOfWeek", "temperature"))
    .setOutputCol("features")

  def extractDateComponents(date: LocalDateTime) = {
    val hour = date.getHour
    val hourAngle = (hour.toFloat / 24) * 2 * Pi
    val hourSin = 50 * sin(hourAngle)
    val hourCos = 50 * cos(hourAngle)
    val dayOfWeek =  // Mon to Friday -> 0, Sat & Sun -> 50
      date.getDayOfWeek match {
        case SATURDAY | SUNDAY => 50
        case _ => 0
      }
   
    (hour, hourSin, hourCos, dayOfWeek)
  }

  def dataDecoder: Array[Byte] => Tuple2[Long,Float] = bytes => {
        val buffer = ByteBuffer.wrap(bytes);
        val epoch = buffer.getLong()
        val value = buffer.getFloat()
        
        (epoch, value)  
      }
  
  def getData(sc: SparkContext, threshold: Float) = {
    val max_voltage = sc.cassandraTable("smartmeter", "max_voltage")
    val table = max_voltage.joinWithCassandraTable("smartmeter", "temperature")

    val flatten = table.map({case (v,t) =>
      val date = LocalDateTime.ofEpochSecond(v.get[Long]("epoch"), 0, ZoneOffset.MIN)
      val voltage = v.get[Float]("voltage")
      val label = (voltage > threshold):Int
      val temperature = t.get[Float]("temperature")
      // https://www.reddit.com/r/MachineLearning/comments/2hzuj5/how_do_i_encode_day_of_the_week_as_a_predictor/
      val (hour, hourSin, hourCos, dayOfWeek) = extractDateComponents(date)
      
      (label, voltage, hour, hourSin, hourCos, dayOfWeek, temperature)})
            
    // @See https://spark.apache.org/docs/2.1.0/api/java/org/apache/spark/sql/SQLContext.implicits$.html
    val sqlContext = SQLContextSingleton.getInstance(sc)
    import sqlContext.implicits._

    val dataframes = flatten.toDF("label", "voltage", "hour", "hourSin", "hourCos", "dayOfWeek", "temperature")
  
    assembler.transform(dataframes)
  }
}


/** Lazily instantiated singleton instance of SQLContext */
// @See https://stackoverflow.com/questions/38833585/spark-streaming-can-not-do-the-todf-function
// @See https://community.hortonworks.com/articles/72941/writing-parquet-on-hdfs-using-spark-streaming.html
import org.apache.spark.sql.SQLContext
object SQLContextSingleton {
 
        @transient  private var instance: SQLContext = _
 
        def getInstance(sparkContext: SparkContext): SQLContext = {
                if (instance == null) {
                        instance = new SQLContext(sparkContext)
                }
                instance
        }
}
