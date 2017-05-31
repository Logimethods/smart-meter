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

import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel

object SparkPredictionProcessor extends App with SparkStreamingProcessor {
  val log = LogManager.getRootLogger
  log.setLevel(Level.WARN)

  val HDFS_URL = System.getenv("HDFS_URL")
  println("HDFS_URL = " + HDFS_URL)
  
  val PREDICTION_MODEL_PATH = HDFS_URL + "/smartmeter/voltage_prediction.model"

  val (properties, target, logLevel, sc, ssc, inputNatsStreaming, inputSubject, outputSubject, clusterId, outputNatsStreaming, natsUrl, streamingDuration) = 
      setupStreaming(args)

  val THRESHOLD = System.getenv("ALERT_THRESHOLD").toFloat
  println("ALERT_THRESHOLD = " + THRESHOLD)
  
  // @See https://spark.apache.org/docs/2.1.0/api/java/org/apache/spark/sql/SQLContext.implicits$.html
//	val sqlContext = new org.apache.spark.sql.SQLContext(sc)
//	import sqlContext.implicits._
	
  import com.datastax.spark.connector._
  
  // http://stackoverflow.com/questions/37513667/how-to-create-a-spark-dataset-from-an-rdd
  // https://github.com/apache/spark/blob/master/examples/src/main/scala/org/apache/spark/examples/ml/MultilayerPerceptronClassifierExample.scala
  val layers = Array[Int](4, 16, 12, 6, 2)
  
  val trainer = new MultilayerPerceptronClassifier().setLayers(layers).setBlockSize(128).setSeed(1234L).setMaxIter(120)
  
  // http://stackoverflow.com/questions/33844591/prepare-data-for-multilayerperceptronclassifier-in-scala
  import java.time.{LocalDateTime, ZoneOffset}
  import scala.math._
  import org.apache.spark.ml.feature.VectorAssembler  

  implicit def bool2int(b:Boolean) = if (b) 1 else 0

  // @See https://spark.apache.org/docs/2.1.0/api/java/org/apache/spark/sql/SQLContext.implicits$.html
  val sqlContext = new org.apache.spark.sql.SQLContext(sc)
  import sqlContext.implicits._  

  val assembler = new VectorAssembler()
    .setInputCols(Array("hourSin", "hourCos", "dayOfWeek", "temperature"))
    .setOutputCol("features")
  
  // Initial training
  val data = getData(sc)
  var model = trainer.fit(data)
  model.write.overwrite.save(PREDICTION_MODEL_PATH)
  println("New model of size " + data.count() + " trained")
  
  new Thread(new Runnable {
              def run() {
                 while( true ){
                   try {
                     val data = getData(sc)
                     model = trainer.fit(data)
                     model.write.overwrite.save(PREDICTION_MODEL_PATH)
                     println("New model of size " + data.count() + " trained")
                     Thread.sleep(streamingDuration)
                   } catch {
                     case e: Throwable => log.error(e)
                   }
                 }
              }
             }).start()
  
  
/*  // @See https://github.com/tyagihas/scala_nats
  import java.util.Properties
  import org.nats._

  val opts : Properties = new Properties
  opts.put("servers", natsUrl);
  val conn = Conn.connect(opts)
  conn.subscribe(inputSubject, 
      (msg:MsgB)  => {
        val buffer = ByteBuffer.wrap(msg.body);
        val epoch = buffer.getLong()
        val temperature = buffer.getFloat()
        
        val date = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN)
        println("Received a message on [" + msg.subject + "] : " + date + " / " + temperature)
      })*/
        
        
  if (! logLevel.startsWith("TEST")) {
    val distributedForecasts =
      if (inputNatsStreaming) {
        NatsToSparkConnector
          .receiveFromNatsStreaming(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY, clusterId)
          .withNatsURL(natsUrl)
          .withSubjects(inputSubject)
          .withDataDecoder(dataDecoder)
          .asStreamOf(ssc)
      } else {
        NatsToSparkConnector
          .receiveFromNats(classOf[Tuple2[Long,Float]], StorageLevel.MEMORY_ONLY)
          .withProperties(properties)
          .withSubjects(inputSubject)
          .withDataDecoder(dataDecoder)
          .asStreamOf(ssc)
      }
  
    val forecasts = distributedForecasts.repartition(1)
    if (logLevel.contains("FORECASTS")) {
      println("FORECASTS will be shown")
//      forecasts.print()
      forecasts.count().print()
    }
    
/*    val dataFrames = forecasts.map( 
        { case (epoch: Long, temperature: Float) =>
            val date = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN)
        		val (hour, hourSin, hourCos, dayOfWeek) = extractDateComponents(date)
        		val values = List((hour, hourSin, hourCos, dayOfWeek, temperature)) 
        		
        		(epoch, temperature, values.toDF("hour", "hourSin", "hourCos", "dayOfWeek", "temperature"))
        })
    
    // @See https://spark.apache.org/docs/2.1.0/streaming-programming-guide.html#accumulators-broadcast-variables-and-checkpoints
    import org.apache.spark.rdd.RDD 
    val predictions = dataFrames.transform { (rdd) =>
        val localModel = Oracle.getInstance(rdd.sparkContext)
        if (localModel.value == null) {
        	throw new RuntimeException("ERROR: The MultilayerPerceptronClassificationModel is not defined")
        }
        
        rdd.map({case (epoch, temperature, dataFrame) =>    
        		val entry = assembler.transform(dataFrame)
        		val prediction = localModel.value.transform(entry).first.getDouble(6) > 0    
        		(epoch, temperature, prediction)
          })
      }*/

    // @See https://spark.apache.org/docs/2.1.0/streaming-programming-guide.html#accumulators-broadcast-variables-and-checkpoints
    // @See https://community.hortonworks.com/articles/72941/writing-parquet-on-hdfs-using-spark-streaming.html
    import org.apache.spark.rdd.RDD 
    val predictions = forecasts.transform { (rdd: RDD[(Long, Float)]) => // foreachRDD
        val localModel = Oracle.getInstance(rdd.sparkContext)
        if (localModel.value == null) {
        	throw new RuntimeException("ERROR: The MultilayerPerceptronClassificationModel is not defined")
        }
        
        val sqlContext = SQLContextSingleton.getInstance(rdd.sparkContext)
        import sqlContext.implicits._

        val predictions = rdd.map({case (epoch: Long, temperature: Float) =>    
 //          prediction(localModel.value: MultilayerPerceptronClassificationModel, epoch: Long, temperature: Float) 
            val date = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN)
        		val (hour, hourSin, hourCos, dayOfWeek) = extractDateComponents(date)
        		val row = List((hour, hourSin, hourCos, dayOfWeek, temperature))
            val dataFrame = row.toDF("hour", "hourSin", "hourCos", "dayOfWeek", "temperature")
            val entry = assembler.transform(dataFrame)
          	val prediction = localModel.value.transform(entry).first.getDouble(6) > 0
        		
        		})
        
//        rdd.sparkContext.parallelize(predictions)
        
        predictions
///        val dataFrames = lists.toDF()
//        log.debug(dataFrames.collect())
///        dataFrames.rdd
 //       		SparkPredictionProcessorHelper.toDS()

//        		val dataFrame = values.toDF("hour", "hourSin", "hourCos", "dayOfWeek", "temperature")
/*        		val entry = assembler.transform(dataFrames)
        		val prediction = localModel.value.transform(entry).first.getDouble(6) > 0    
        		(epoch, temperature, prediction)*/
          }
    
   predictions.print()
    
/*    if (logLevel.contains("PREDICTIONS")) {
      println("PREDICTIONS will be shown")
      predictions.print()
    }  
    
    val alerts = predictions.filter(_._3).map({case (epoch, temperature, flag) => predictionMessage(epoch, temperature) })

    SparkToNatsConnectorPool.newPool()
                            .withProperties(properties)
                            .withSubjects(outputSubject)
                            .publishToNats(alerts) */
    
    // Start //
    println("Start Predictions")
    
    ssc.start();		   
    ssc.awaitTermination()
    
  } else {    
    val data = getData(sc)
    data.show()
    
    val splits = data.randomSplit(Array(0.6, 0.4), seed = 1234L)
    val train = splits(0)
    val test = splits(1)
    
    val model = trainer.fit(train)
    
    val result = model.transform(test)
    
    result.show()
    
    val predictionAndLabels = result.select("prediction", "label")
    val evaluator = new MulticlassClassificationEvaluator().setMetricName("accuracy")
    
    println("Size: " + data.count())
    println("Test Set Accuracy = " + evaluator.evaluate(predictionAndLabels))
  }
  
  def prediction(localModel: MultilayerPerceptronClassificationModel, epoch: Long, temperature: Float) = {
    val date = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN)
		val (hour, hourSin, hourCos, dayOfWeek) = extractDateComponents(date)
		val values = List((hour, hourSin, hourCos, dayOfWeek, temperature))
		
		import org.apache.spark.sql._
		val sqlContext = new org.apache.spark.sql.SQLContext(sc)
		import sqlContext.implicits._  
		
		val dataFrame = values.toDF("hour", "hourSin", "hourCos", "dayOfWeek", "temperature")
		val entry = assembler.transform(dataFrame)
		val prediction = localModel.transform(entry).first.getDouble(6) > 0        
    (epoch, temperature, prediction)
  }

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
  
  def getData(sc: SparkContext) = {
    val max_voltage = sc.cassandraTable("smartmeter", "max_voltage")
    val table = max_voltage.joinWithCassandraTable("smartmeter", "temperature")

    val flatten = table.map({case (v,t) =>
      val date = LocalDateTime.ofEpochSecond(v.get[Long]("epoch"), 0, ZoneOffset.MIN)
      val voltage = v.get[Float]("voltage")
      val label = (voltage > THRESHOLD):Int
      val temperature = t.get[Float]("temperature")
      // https://www.reddit.com/r/MachineLearning/comments/2hzuj5/how_do_i_encode_day_of_the_week_as_a_predictor/
      val (hour, hourSin, hourCos, dayOfWeek) = extractDateComponents(date)
      
      (label, voltage, hour, hourSin, hourCos, dayOfWeek, temperature)})
            
    val dataframes = flatten.toDF("label", "voltage", "hour", "hourSin", "hourCos", "dayOfWeek", "temperature")
  
    assembler.transform(dataframes)
  }
  
  // @See https://spark.apache.org/docs/2.1.0/streaming-programming-guide.html#accumulators-broadcast-variables-and-checkpoints
  import org.apache.spark.broadcast.Broadcast
  object Oracle {
  
    @volatile private var instance: Broadcast[MultilayerPerceptronClassificationModel] = null
  
    def getInstance(sc: SparkContext): Broadcast[MultilayerPerceptronClassificationModel] = {
      if (instance == null) {
        synchronized {
          if (instance == null) {
            val oracle = MultilayerPerceptronClassificationModel.load(PREDICTION_MODEL_PATH)
            println("MultilayerPerceptronClassificationModel loaded")
            instance = sc.broadcast(oracle)
          }
        }
      }
      instance
    }
  }

  def predictionMessage(epoch: Long, temperature: Float) = {
    val timestamp = epoch * 1000
    s"""{"timestamp":$timestamp,"temperature":$temperature,"alert": $THRESHOLD}"""
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
}