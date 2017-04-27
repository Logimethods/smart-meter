package com.logimethods.smartmeter.generate

import com.logimethods.smartmeter.generate._
import org.scalatest._
import java.time._
import Math._

class TemperatureProviderTest extends FunSuite {

  test("temperature") {
    val firstDay = LocalDateTime.of(2017, 1, 1, 0, 0)
    val lastDay = LocalDateTime.of(2017, 12, 31, 23, 59)
    var day = LocalDateTime.of(2017, 1, 1, 0, 0)
    var temp = TemperatureProvider.temperature(firstDay, 0)
    while (day.isBefore(lastDay)) {
      day = day.plusHours(1)
      val newTemp = TemperatureProvider.temperature(day, 0)
//      println(day + " (" + day.getDayOfWeek.ordinal + ") : " + newTemp + " / " + abs(newTemp - temp))
//     println(newTemp)
      assert(abs(newTemp - temp) < 2)
      temp = newTemp
    }

  }
}