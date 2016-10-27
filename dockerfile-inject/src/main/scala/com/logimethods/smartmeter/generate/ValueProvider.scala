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
  
  val usagePointPK = 1
  val rndValue = 0
  
  val incr = 15
  var date = LocalDateTime.now()
  
  def getSubject(): String = {
    return "." + usagePointPK.toString()
  }
  
  def getPayload(): Array[Byte] = {
    date = date.plusMinutes(incr)
    val value = ConsumerInterpolatedVoltageProfile.valueAtDayAndHour(usagePointPK, date.getDayOfWeek().ordinal(), date.getHour(), rndValue)
    return value.toString().getBytes()
  }
}