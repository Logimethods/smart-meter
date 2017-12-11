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
import java.time.DayOfWeek._

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
  var date =  LocalDateTime.ofEpochSecond(TimeProvider.time(), 0, ZoneOffset.MIN)
  var tictac = true

  val defaultValues: Array[Float] = Array(100.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f)

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
    temperatures += TemperatureProvider.temperature(date, random.nextFloat())
    temperatures.dequeue()
  }

  def forecast() = {
    temperatures.last
  }

  def voltage() = {
    val baseVoltage = ConsumerInterpolatedVoltageProfile
                          .valueAtDayAndHour(point(),
                                            date.getDayOfWeek(),
                                            date.getHour(),
                                            randomness * (random.nextFloat() - 0.5f))

    baseVoltage + ((temperatures.front -5).abs * 0.08f) - 1
  }

  def increment() {
    date = LocalDateTime.ofEpochSecond(TimeProvider.time(), 0, ZoneOffset.MIN)
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
    val buffer = returnType match {
      case ReturnType.VOLTAGE => ByteBuffer.allocate(8+(4*12))
      case _ => ByteBuffer.allocate(8+4)
    }

    returnType match {
      case ReturnType.FORECAST => buffer.putLong(date.plusHours(12).atOffset(ZoneOffset.MIN).toEpochSecond())
      case _ => buffer.putLong(date.atOffset(ZoneOffset.UTC).toEpochSecond())
    }

    buffer.putFloat(value)

    returnType match {
      case ReturnType.VOLTAGE =>
        for (i <- 0 to (defaultValues.length - 1)) { buffer.putFloat(defaultValues(i) + random.nextFloat()) }
      case _ =>
    }

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