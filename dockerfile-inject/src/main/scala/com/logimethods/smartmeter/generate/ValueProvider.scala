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

class LoopingValueProvider {
  
  val incr = 10
  val basedValue = 100 -incr
  val maxIncr = 50
  var actualIncr = 0
  
  override def toString(): String = {
    actualIncr = (actualIncr % (maxIncr + incr)) + incr
    (basedValue + actualIncr).toString()
  }
}

class ConsumerInterpolatedVoltageProvider extends NatsMessage {
  import java.time._
  import scala.math._

  val random = scala.util.Random

  var line = 1
  var transformer = 1
  var usagePoint = 1
  
  val incr = 15
  var date = LocalDateTime.now()
  val zoneId = ZoneId.systemDefault(); 
  
  def getSubject(): String = {
    return "." + point()
  }
  
  def getPayload(): Array[Byte] = {
    if (usagePoint > 3) {
      usagePoint = 1
      transformer += 1
    }
    if (transformer > 3) {
      transformer = 1
      line += 1
    }
    if (line > 3) {
      line = 1
      date = date.plusMinutes(incr)
    }
    
    val value = InterpolatedProfileByUsagePoint.voltageAtDayAndHour(point(), date.getDayOfWeek().ordinal(), date.getHour(), (random.nextFloat() - 0.5f))
    
    usagePoint += 1

    return encodePayload(date, value)
  }
  
  def encodePayload(date: LocalDateTime, value: Float): Array[Byte] = {
    // https://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html
    val buffer = ByteBuffer.allocate(4+8);
    buffer.putLong(date.atZone(zoneId).toEpochSecond())
    buffer.putFloat(value)
    
    return buffer.array()    
  }
  
  def point(): String = {
    return (line + "." + transformer + "." + usagePoint)
  }
}