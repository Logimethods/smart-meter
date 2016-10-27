/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.smartmeter.generate

import math._

trait Profile extends Serializable {
  def demandAtDayAndHour(usagePointPK: Long, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float  
  def voltageAtDayAndHour(usagePointPK: Long, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float
}

class DefaultProfile extends Profile {
  def demandAtDayAndHour(usagePointPK: Long, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float = {
    math.abs(rndValue * (usagePointPK % 1000))
  }

  def voltageAtDayAndHour(usagePointPK: Long, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float = {
    ((rndValue * 2) + (hourInDay / 2.4) + 113).toFloat
  } 
}

class RandnProfile extends Profile {
  def demandAtDayAndHour(usagePointPK: Long, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float = {
    rndValue
  }

  def voltageAtDayAndHour(usagePointPK: Long, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float = {
    rndValue
  } 
}

class ProfileByUsagePoint extends Profile {
  val caseNb = 10
  val caseFn = (usagePointPK: Long) => usagePointPK % caseNb

  def demandAtDayAndHour(usagePointPK: Long, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float = caseFn(usagePointPK) match {
    case 0 => math.abs(rndValue * (usagePointPK % 100))
    case 1 => math.abs(rndValue * (usagePointPK % 150))
    case 2 => math.abs(rndValue * (usagePointPK % 200))
    case 3 => math.abs(rndValue * (usagePointPK % 250))
    case 4 => math.abs(rndValue * (usagePointPK % 300))
    case 5 => math.abs(rndValue * (usagePointPK % 800))
    case 6 => math.abs(rndValue * (usagePointPK % 850))
    case 7 => math.abs(rndValue * (usagePointPK % 900))
    case 8 => math.abs(rndValue * (usagePointPK % 950))
    case 9 => math.abs(rndValue * (usagePointPK % 1000))
  }

  def voltageAtDayAndHour(usagePointPK: Long, dayInWeek: Int, hourInDay: Int, rndValue: Float): Float = caseFn(usagePointPK) match {
    case 0 => ((rndValue * 2) + (hourInDay / 2.4) + 100).toFloat
    case 1 => ((rndValue * 2) + (hourInDay / 2.4) + 101).toFloat
    case 2 => ((rndValue * 2) + (hourInDay / 2.4) + 102).toFloat
    case 3 => ((rndValue * 2) + (hourInDay / 2.4) + 103).toFloat
    case 4 => ((rndValue * 2) + (hourInDay / 2.4) + 104).toFloat
    case 5 => ((rndValue * 2) + (hourInDay / 2.4) + 109).toFloat
    case 6 => ((rndValue * 2) + (hourInDay / 2.4) + 110).toFloat
    case 7 => ((rndValue * 2) + (hourInDay / 2.4) + 111).toFloat
    case 8 => ((rndValue * 2) + (hourInDay / 2.4) + 112).toFloat
    case 9 => ((rndValue * 2) + (hourInDay / 2.4) + 113).toFloat
  } 
}


/*
val newMeasures = new MeasureBuilderByProfile().build(powerSystemResourceDim, timeDim)
val newMeasures = new MeasureBuilderByProfile(new ProfileByUsagePoint).build(powerSystemResourceDim, timeDim)
newMeasures.first()
*/