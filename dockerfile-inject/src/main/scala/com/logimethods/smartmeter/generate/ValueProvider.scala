/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.smartmeter.generate

import com.logimethods.connector.gatling.to_nats.NatsMessage
import java.nio.ByteBuffer
import math._

class ConsumerInterpolatedVoltageProvider(slot: Int, usersPerSec: Double, streamingDuration: Int, 
                                          randomness: Float, predictionLength: Int) extends NatsMessage {
  import java.time._
  import scala.math._
  import scala.collection.mutable.Queue

  val random = scala.util.Random
  
  val (lineNb, transformerNb, usagePointNb) = ProviderUtil.computeNbOfElements(usersPerSec)

  var line = lineNb
  var transformer = transformerNb
  var usagePoint = usagePointNb + 1
  
  val incr = ProviderUtil.computeIncr(streamingDuration)
  var date = LocalDateTime.now()
  var tictac = true
  
  sealed trait ReturnType
  object ReturnType {
    case object VOLTAGE extends ReturnType;
    case object TEMPERATURE extends ReturnType;
    case object FORECAST extends ReturnType;
  }
  
  var returnType: ReturnType = ReturnType.TEMPERATURE
  
  var temperatures = Queue(5.0f)
  for (i <- 1 until predictionLength) {
    temperatures += (temperatures.front + (random.nextFloat() - 0.5f))
  }
  
  def temperature() = {
    val last = temperatures.last
    val dayOfWeek = date.getDayOfWeek.ordinal + 1
    val bias = ( if (date.getDayOfYear % 2 == 0) (8 - dayOfWeek) else - dayOfWeek ).toFloat / 6
    temperatures += (last + (bias * (random.nextFloat() - 0.2f)))
    temperatures.dequeue()
  }
  
  def forecast() = {
    temperatures.last
  }
  
  def voltage() = {
    val baseVoltage = ConsumerInterpolatedVoltageProfile
                          .valueAtDayAndHour(point(), 
                                            date.getDayOfWeek().ordinal(),
                                            date.getHour(),
                                            randomness * (random.nextFloat() - 0.5f))
                                            
    baseVoltage + ((temperatures.front -5).abs * 0.08f) - 1
  }
  
  def increment() {
    if (tictac) {
      if (returnType == ReturnType.TEMPERATURE) {
        returnType = ReturnType.FORECAST
      } else {
        if (usagePoint > usagePointNb) {
          usagePoint = 1
          transformer += 1
        }
        if (transformer > transformerNb) {
          transformer = 1
          line += 1
        }
        if (line > lineNb) {
          line = 1
          date = date.plusMinutes(incr)
          returnType = ReturnType.TEMPERATURE
        } else {
          returnType = ReturnType.VOLTAGE
        }
      }
    }
    tictac = ! tictac
  }
      
  def getSubject() = { 
    increment()
    returnType match {
      case ReturnType.TEMPERATURE => ".temperature"
      case ReturnType.FORECAST => ".forecast.12"
      case ReturnType.VOLTAGE => ".data." + point()
    }
  }
  
  def getPayload(): Array[Byte] = {
    increment()
    val value = returnType match {
      case ReturnType.TEMPERATURE => temperature()
      case ReturnType.FORECAST => forecast()
      case ReturnType.VOLTAGE => voltage()
    }
    
    usagePoint += 1

    return encodePayload(date, value)
  }
  
  def encodePayload(date: LocalDateTime, value: Float): Array[Byte] = {
    // https://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html
    val buffer = ByteBuffer.allocate(8+4);
    returnType match {
      case ReturnType.FORECAST => buffer.putLong(date.plusHours(12).atOffset(ZoneOffset.MIN).toEpochSecond())
      case _ => buffer.putLong(date.atOffset(ZoneOffset.MIN).toEpochSecond())
    }
    buffer.putFloat(value)
    
    return buffer.array()    
  }
  
  def point(): String = {
    return ((10*slot) + line + "." + transformer + "." + usagePoint)
  }
}

object ProviderUtil {
  
  def computeNbOfElements(usersPerSec: Double) = {
    val lineNtransformer = (math.cbrt(usersPerSec)).toInt
    val lineNb = (math.cbrt(lineNtransformer)).toInt
    val transformerNb = (lineNtransformer / lineNb).toInt
    val usagePointNb = (usersPerSec / (lineNb * transformerNb)).toInt
    
    print("computeNbOfElements: ")
    println (lineNb, transformerNb, usagePointNb)
    
    (lineNb, transformerNb, usagePointNb)
  }  
  
  def computeIncr(streamingDuration: Int) = {
    60000 / streamingDuration
  }
}