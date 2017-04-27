package com.logimethods.smartmeter.generate

import java.time._
import Math._

object TemperatureProvider {
    
  def temperature(date: LocalDateTime, rnd: Float): Float = {
//    println()
    
    val annualVariation = -22.0f * cos((2 * PI * (date.getDayOfYear - 1)) / 365) 
//    println("annualVariation :" + annualVariation)
    val dailyVariation = 1.0f * cos((2 * PI * (date.getDayOfWeek.ordinal - 1)) / 6) 
//    println("dailyVariation :" + dailyVariation)
//    println("-" + ((2 * PI * (date.getDayOfWeek.ordinal - 1)) / 6) + "->" + cos((2 * PI * (date.getDayOfWeek.ordinal - 1)) / 6))
    val hourlyVariation = -5.0f * cos((2 * PI * date.getHour) / 23) 
//    println("hourlyVariation :" + hourlyVariation)
    val rndVariation = 2.0f * rnd
    
    (annualVariation + dailyVariation + hourlyVariation + rndVariation).toFloat
  }

}