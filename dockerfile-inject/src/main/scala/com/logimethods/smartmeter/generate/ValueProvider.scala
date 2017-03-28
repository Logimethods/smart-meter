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

/*class LoopingValueProvider {
  
  val incr = 10
  val basedValue = 100 -incr
  val maxIncr = 50
  var actualIncr = 0
  
  override def toString(): String = {
    actualIncr = (actualIncr % (maxIncr + incr)) + incr
    (basedValue + actualIncr).toString()
  }
}*/

class ConsumerInterpolatedVoltageProvider(slot: Int, usersPerSec: Double, streamingDuration: Int) extends NatsMessage {
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
  var returnTemperature = true
  var temperatures = Queue(5.0f)
  for (i <- 1 until 12) {
    temperatures += (temperatures.front + (random.nextFloat() - 0.5f))
  }
  
  def temperature() = {
    val last = temperatures.last
    val dayOfWeek = date.getDayOfWeek.ordinal + 1
    val bias = ( if (date.getDayOfYear % 2 == 0) (8 - dayOfWeek) else - dayOfWeek ).toFloat / 6
    temperatures += (last + (bias * (random.nextFloat() - 0.2f)))
    temperatures.dequeue()
  }
  
  def increment() {
    if (tictac) {
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
        returnTemperature = true
      } else {
        returnTemperature = false
      }
    }
    tictac = ! tictac
    print("tictac: " + tictac) 
    println
  }
      
  def getSubject() = { 
    increment()
    if (returnTemperature) ".temperature" else ".data." + point()
  }
  
  def getPayload(): Array[Byte] = {
    increment()
    val value = 
      if (returnTemperature) temperature()
         else ConsumerInterpolatedVoltageProfile.valueAtDayAndHour(point(), date.getDayOfWeek().ordinal(), date.getHour(), (random.nextFloat() - 0.5f)) 
    
    usagePoint += 1

    return encodePayload(date, value)
  }
  
  def encodePayload(date: LocalDateTime, value: Float): Array[Byte] = {
    // https://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html
    val buffer = ByteBuffer.allocate(8+4);
    buffer.putLong(date.atOffset(ZoneOffset.MIN).toEpochSecond())
    buffer.putFloat(value)
    
    //print(buffer.array().deep)    
    //println
    
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