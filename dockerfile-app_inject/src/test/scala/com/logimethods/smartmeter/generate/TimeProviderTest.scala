package com.logimethods.smartmeter.generate

import com.logimethods.smartmeter.generate._
import org.scalatest._
import java.time._

class TimeProviderTest extends FunSuite {

  test("getConfigTime") {
    val date = LocalDateTime.now().atOffset(ZoneOffset.MIN).toEpochSecond()
    val root = 1493105656L
    TimeProvider.config = Some(root)
    val value = TimeProvider.time()
    
    println(root)
    println(value)
    
/*    val delta = date - root
    println("delta: " + delta)
    val newEpoch = root + (delta * 60 * 15)
    val newDate = LocalDateTime.ofEpochSecond(newEpoch, 0, ZoneOffset.MIN)
    println(LocalDateTime.now())
    println(newDate)*/
    
//    assert(date.atOffset(ZoneOffset.MIN).toEpochSecond() == tuple._1)
//    assert(date.withNano(0) == LocalDateTime.ofEpochSecond(tuple._1, 0, ZoneOffset.MIN))
    assert(root <= value)
    
    var oldDate = LocalDateTime.ofEpochSecond(TimeProvider.time(), 0, ZoneOffset.MIN)
    for (a <- 0 to 5) {
      Thread.sleep(1000)
      val time = TimeProvider.time()
      val newDate = LocalDateTime.ofEpochSecond(time, 0, ZoneOffset.MIN)
//      println(time + " : " + newDate)
      assert(oldDate.plusMinutes(15) == newDate)
      oldDate = newDate
    }
  }

/*  test("getDefaultTime") {
    val date = LocalDateTime.now().atOffset(ZoneOffset.MIN).toEpochSecond()
    val value = TimeProvider.time()
    
    println(date)
    println(value)
    
//    assert(date.atOffset(ZoneOffset.MIN).toEpochSecond() == tuple._1)
//    assert(date.withNano(0) == LocalDateTime.ofEpochSecond(tuple._1, 0, ZoneOffset.MIN))
    assert(date == value)
  }*/
}