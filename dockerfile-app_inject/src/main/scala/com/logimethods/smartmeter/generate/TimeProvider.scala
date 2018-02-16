package com.logimethods.smartmeter.generate

import java.time._

object TimeProvider {

  // See http://stackoverflow.com/questions/8782448/how-do-i-initialize-object-vals-with-values-known-only-at-runtime
  val defaultRoot = LocalDateTime.now().atOffset(ZoneOffset.MIN).toEpochSecond()
  var config: Option[Long] = None 
  lazy val root = config.getOrElse(defaultRoot)
    
  def time() = {
    val date = LocalDateTime.now().atOffset(ZoneOffset.MIN).toEpochSecond()
    val delta = date - root
    root + (delta * 60 * 15)
  }
}