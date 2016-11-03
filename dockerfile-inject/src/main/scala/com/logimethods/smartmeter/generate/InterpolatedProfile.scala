/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.smartmeter.generate

import breeze.interpolation._
import breeze.linalg._

// https://github.com/scalanlp/breeze/wiki/Interpolation
abstract class InterpolatedProfile {
  def vectorRange(vector: DenseVector[Double]): Double = {
    var min = Double.MaxValue
    var max = Double.MinPositiveValue
    for (value <- vector){
      if (value > max) { max = value }
      if (value < min) { min = value }
    }
    max - min
  }
  val hours = DenseVector(0.0 to 24.0 by 3.0 toArray)
  
  val weekValues: DenseVector[Double]
  lazy val weekRange = vectorRange(weekValues)
  lazy val weekFunctionInterpolator = LinearInterpolator(hours, weekValues)
  def weekFunction(hour: Double) = weekFunctionInterpolator(hour)
  
  val weekendValues: DenseVector[Double]
  lazy val weekendRange = vectorRange(weekendValues)
  lazy val weekendFunctionInterpolator = LinearInterpolator(hours, weekendValues)
  def weekendFunction(hour: Double) =  weekendFunctionInterpolator(hour)

  // Utilisé pour créer un biais systématique par usagePointPK + dayInWeek + hourInDay
  def bias(usagePointPK: String, dayInWeek: Int, hourInDay: Int, rndValue: Float, range: Double) = {
    2.0 * range * (rndValue + (((usagePointPK.hashCode() + dayInWeek + hourInDay) % 20) / 20) - 0.5)
  }

  def valueAtDayAndHour(usagePointPK: String, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float = dayInWeek match {
    case 1 | 2 | 3 | 4 | 5 => 
            math.abs((bias(usagePointPK, dayInWeek, hourInDay, rndValue, weekRange) + weekFunction(hourInDay)).toFloat)
    case 0 | 6 => 
            math.abs((bias(usagePointPK, dayInWeek, hourInDay, rndValue, weekendRange) + weekendFunction(hourInDay)).toFloat)
  }  
}

/**
 * Consumer
 */
object ConsumerInterpolatedDemandProfile extends InterpolatedProfile {
  val weekValues = DenseVector(/*0am*/ 100.0, /*3am*/ 100.0, /*6am*/ 80.0, /*9am*/ 50.0, /*12am*/ 60.0,
                               /*3pm*/ 80.0, /*6pm*/ 90.0, /*9pm*/ 115.0, /*12pm*/ 100.0)

  val weekendValues = DenseVector(/*0am*/ 100.0, /*3am*/ 100.0, /*6am*/ 80.0, /*9am*/ 90.0, /*12am*/ 80.0,
                                  /*3pm*/ 90.0, /*6pm*/ 115.0, /*9pm*/ 90.0, /*12pm*/ 100.0)
}

object ConsumerInterpolatedVoltageProfile extends InterpolatedProfile {
  val weekValues = DenseVector(/*0am*/ 117.0, /*3am*/ 117.0, /*6am*/ 118.0, /*9am*/ 116.0, /*12am*/ 115.0,
                               /*3pm*/ 114.0, /*6pm*/ 116.0, /*9pm*/ 118.0, /*12pm*/ 117.0)

  val weekendValues = DenseVector(/*0am*/ 117.0, /*3am*/ 117.0, /*6am*/ 116.0, /*9am*/ 116.0, /*12am*/ 118.0,
                                  /*3pm*/ 119.0, /*6pm*/ 119.0, /*9pm*/ 118.0, /*12pm*/ 117.0)
}

/**
 * Business
 */

object BusinessInterpolatedDemandProfile extends InterpolatedProfile {
  val weekValues = DenseVector(/*0am*/ 60.0, /*3am*/ 80.0, /*6am*/ 160.0, /*9am*/ 280.0, /*12am*/ 300.0,
                               /*3pm*/ 290.0, /*6pm*/ 180.0, /*9pm*/ 80.0, /*12pm*/ 60.0)

  val weekendValues = DenseVector(/*0am*/ 100.0, /*3am*/ 80.0, /*6am*/ 70.0, /*9am*/ 70.0, /*12am*/ 60.0,
                                  /*3pm*/ 70.0, /*6pm*/ 80.0, /*9pm*/ 90.0, /*12pm*/ 100.0)
}

object BusinessInterpolatedVoltageProfile extends InterpolatedProfile {
  val weekValues = DenseVector(/*0am*/ 117.0, /*3am*/ 117.0, /*6am*/ 118.0, /*9am*/ 119.0, /*12am*/ 119.0,
                               /*3pm*/ 120.0, /*6pm*/ 118.0, /*9pm*/ 117.0, /*12pm*/ 117.0)

  val weekendValues = DenseVector(/*0am*/ 117.0, /*3am*/ 116.0, /*6am*/ 116.0, /*9am*/ 115.0, /*12am*/ 114.0,
                                  /*3pm*/ 114.0, /*6pm*/ 114.0, /*9pm*/ 115.0, /*12pm*/ 117.0)
}

/**
 * Industry
 */

object IndustryInterpolatedDemandProfile extends InterpolatedProfile {
  val weekValues = DenseVector(/*0am*/ 3000.0, /*3am*/ 3000.0, /*6am*/ 3200.0, /*9am*/ 3300.0, /*12am*/ 3200.0,
                               /*3pm*/ 3300.0, /*6pm*/ 3100.0, /*9pm*/ 3000.0, /*12pm*/ 3000.0)

  val weekendValues = DenseVector(/*0am*/ 2700.0, /*3am*/ 2700.0, /*6am*/ 2750.0, /*9am*/ 2800.0, /*12am*/ 2800.0,
                                  /*3pm*/ 2850.0, /*6pm*/ 2750.0, /*9pm*/ 2700.0, /*12pm*/ 2700.0)
}

object IndustryInterpolatedVoltageProfile extends InterpolatedProfile {
  val weekValues = DenseVector(/*0am*/ 120.0, /*3am*/ 120.0, /*6am*/ 121.0, /*9am*/ 122.0, /*12am*/ 121.0,
                               /*3pm*/ 122.0, /*6pm*/ 121.0, /*9pm*/ 121.0, /*12pm*/ 120.0)

  val weekendValues = DenseVector(/*0am*/ 120.0, /*3am*/ 120.0, /*6am*/ 121.0, /*9am*/ 121.0, /*12am*/ 121.0,
                                  /*3pm*/ 121.0, /*6pm*/ 120.0, /*9pm*/ 120.0, /*12pm*/ 120.0)
}


/**
 * InterpolatedProfileByUsagePoint
 */
class InterpolatedProfileByUsagePoint extends Profile {
  val caseNb = 10
  val caseFn = (usagePointPK: String) => usagePointPK.hashCode() % caseNb

  def demandAtDayAndHour(usagePointPK: String, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float = caseFn(usagePointPK) match {
    case 0 | 1 | 2 => BusinessInterpolatedDemandProfile.valueAtDayAndHour(usagePointPK, dayInWeek, hourInDay, rndValue)
    case 3 | 4 => IndustryInterpolatedDemandProfile.valueAtDayAndHour(usagePointPK, dayInWeek, hourInDay, rndValue)
    case 5 | 6 | 7 | 8 | 9 => ConsumerInterpolatedDemandProfile.valueAtDayAndHour(usagePointPK, dayInWeek, hourInDay, rndValue)
  }

  def voltageAtDayAndHour(usagePointPK: String, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float = caseFn(usagePointPK) match {
    case 0 | 1 | 2 => BusinessInterpolatedVoltageProfile.valueAtDayAndHour(usagePointPK, dayInWeek, hourInDay, rndValue)
    case 3 | 4 => IndustryInterpolatedVoltageProfile.valueAtDayAndHour(usagePointPK, dayInWeek, hourInDay, rndValue)
    case 5 | 6 | 7 | 8 | 9 => ConsumerInterpolatedVoltageProfile.valueAtDayAndHour(usagePointPK, dayInWeek, hourInDay, rndValue)
  } 
}
