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

object SparkMaxProcessor extends App with SparkProcessor {
  val log = LogManager.getRootLogger
  log.setLevel(Level.WARN)
  
  val (properties, logLevel, ssc, inputStreaming, inputSubject, outputSubject, clusterId, outputStreaming, natsUrl) = setup(args)
  
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
  
  // MAXIMUM values
  
  val max = messages.reduceByKey((t1, t2) => (Math.max(t1._1,t2._1), Math.max(t1._2,t2._2)))

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
                          
  // Predictions
  
/*  // Update the cumulative count using mapWithState
  // This will give a DStream made of state (which is the cumulative count of the words)
  val mappingFunc = (word: String, one: Option[Int], state: State[Int]) => {
    val sum = one.getOrElse(0) + state.getOption.getOrElse(0)
    val output = (word, sum)
    state.update(sum)
    output
  }

    // A mapping function that maintains an integer state and returns a String
    def mappingFunction(key: String, value: Option[Int], state: State[Int]): Option[String] = {
      // Check if state exists
      if (state.exists) {
        val existingState = state.get  // Get the existing state
        val shouldRemove = ...         // Decide whether to remove the state
        if (shouldRemove) {
          state.remove()     // Remove the state
        } else {
          val newState = ...
          state.update(newState)    // Set the new state
        }
      } else {
        val initialState = ...
        state.update(initialState)  // Set the initial state
      }
      ... // return something
    }*/
  
  val maxDelta = max.map(
      {case (subject, (epoch, voltage)) 
          => (/*LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN).getDayOfWeek, */
                        LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.MIN).getHour, 
                        voltage) })
  //maxDelta.print()
  
  val maxOfMax = maxDelta.reduceByKey(Math.max(_, _))
  
  import org.apache.spark.HashPartitioner
  val numOfPartitions = 24
  val partitioner = new HashPartitioner(numOfPartitions)

  val sumCount = maxDelta.combineByKey((v) => (v, 1),
                                       (x:(Float, Int), value) => (x._1 + value, x._2 + 1),
                                       (x:(Float, Int), y:(Float, Int)) => (x._1 + y._1, x._2 + y._2),
                                       partitioner)
  
  val averageByKey = sumCount.map({case (key, value) => (key, value._1 / value._2)})
//  averageByKey.print()
  
  // @See http://stackoverflow.com/questions/38961581/best-solution-to-accumulate-spark-streaming-dstream      
  // @See http://asyncified.io/2016/07/31/exploring-stateful-streaming-with-apache-spark/                      
  case class HourlyVoltage(/*dayOfWeek: Int,*/ hour: Int, voltage: Float)
  
//  case class HourlyVoltageSeq(hourlyVoltages: Seq[HourlyVoltage])
  import scala.collection.mutable.MutableList
  
  def statefulTransformation(key: Int,
                           value: Option[Float],
                           state: State[MutableList[Float]]): Option[Float] = {
    def updateState(value: Float): Float = {
      val updatedList =
        state
          .getOption()
          .map(list => list :+ value)
        .getOrElse(MutableList(value))

      state.update(updatedList)
      value
    }
  
    value.map(updateState)
  }
  
  val stateSpec = StateSpec.function(statefulTransformation _)
  val maxStats = averageByKey.mapWithState(stateSpec)
  maxStats.stateSnapshots().print()
  maxStats.stateSnapshots().saveAsTextFiles("/spark/storage/text/maxstats", "txt")
  maxStats.stateSnapshots().saveAsObjectFiles("/spark/storage/obj/maxstats", "obj")
        
  // Start
  ssc.start();		
  
  ssc.awaitTermination()
}