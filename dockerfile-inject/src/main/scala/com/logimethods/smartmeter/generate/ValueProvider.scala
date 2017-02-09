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

class ConsumerInterpolatedVoltageProvider(usersPerSec: Double) extends NatsMessage {
  import java.time._
  import scala.math._

  val random = scala.util.Random
  
  val (lineNb, transformerNb, usagePointNb) = ProviderUtil.computeNbOfElements(usersPerSec)

  var line = 1
  var transformer = 1
  var usagePoint = 1
  
  val incr = 15
  var date = LocalDateTime.now()
      
  def getSubject(): String = {
    return "." + point()
  }
  
  def getPayload(): Array[Byte] = {
    if (usagePoint > usagePointNb) {
      usagePoint = 1
      transformer += 1
    }
    if (transformer > transformerNb) {
      transformer = 1
      line += 1
    }
    if (line > lineNb) {
//      println(s"$line, $transformer, $usagePoint")
      line = 1
      date = date.plusMinutes(incr)
//      println(date)
    }
    
    val value = InterpolatedProfileByUsagePoint.voltageAtDayAndHour(point(), date.getDayOfWeek().ordinal(), date.getHour(), (random.nextFloat() - 0.5f))
    
    usagePoint += 1

    return encodePayload(date, value)

    /*
    val rnd = (random.nextFloat() - 0.5f)
    try { 
      val value = InterpolatedProfileByUsagePoint.voltageAtDayAndHour(point(), date.getDayOfWeek().ordinal(), date.getHour(), rnd);
      usagePoint += 1
    		  
      return encodePayload(date, value)      
    } 
    catch {
      case e: Throwable => 
            val p = point()
            val o = date.getDayOfWeek().ordinal()
            val h = date.getHour()
            println(s"voltageAtDayAndHour FAILED: point= $p , ordinal = $o , hour = $h , random = $rnd")
            e.printStackTrace()
            
    	      usagePoint += 1   	      
    	      return encodePayload(date, 0.0f)      
    }   
    */
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
    return (line + "." + transformer + "." + usagePoint)
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
}