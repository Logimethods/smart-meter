/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.nats.connector.spark.app

import java.util.Properties

// @see https://github.com/tyagihas/scala_nats
object DummyNatsTransmiter /*extends App*/ {
/*  Thread.sleep(2000)
  
  val properties = new Properties()
  //@see https://github.com/tyagihas/java_nats/blob/master/src/main/java/org/nats/Connection.java
  properties.put("servers", "nats://nats-main:4222")
  val conn = Conn.connect(properties)

  val inputSubject = args(0)
  val outputSubject = args(1)  
  println("Will transmit messages from " + inputSubject + " to " + outputSubject)

  conn.subscribe(inputSubject, (msg:Msg) => {
    println("Transmiting message from " + inputSubject + " to " + outputSubject + ": " + msg.body)
    conn.publish(outputSubject, msg.body)
    })*/
}