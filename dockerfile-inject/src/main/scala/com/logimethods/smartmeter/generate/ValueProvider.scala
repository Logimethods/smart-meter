/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.smartmeter.generate

import com.logimethods.connector.gatling.to_nats.NatsMessage

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

  val rndValue = 0

  var line = 1
  var transformer = 1
  var usagePoint = 1
  var point = line + "." + usagePoint + "." + usagePoint
  
  val incr = 15
  var date = LocalDateTime.now()
  
  def getSubject(): String = {
    return "." + point
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
    
    point = line + "." + usagePoint + "." + usagePoint
    val value = ConsumerInterpolatedVoltageProfile.valueAtDayAndHour(point, date.getDayOfWeek().ordinal(), date.getHour(), rndValue)
    
    usagePoint += 1

    return value.toString().getBytes()
  }
}