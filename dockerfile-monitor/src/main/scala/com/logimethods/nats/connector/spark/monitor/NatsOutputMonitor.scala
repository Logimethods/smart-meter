/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.nats.connector.spark.monitor

import java.util.Properties
import org.nats._
import java.nio.ByteBuffer;

// @see https://github.com/tyagihas/scala_nats
object NatsOutputMonitor extends App {
  Thread.sleep(2000)

  val inputSubject = args(0)
  println("Will be listening to messages from " + inputSubject)
    
  val natsUrl = System.getenv("NATS_URI")
  println("NATS_URI = " + natsUrl)
  
  if (inputSubject.toUpperCase().contains("STREAMING")) {
    val clusterID = System.getenv("NATS_CLUSTER_ID");
		System.out.println("NATS_CLUSTER_ID = " + clusterID);

    NatsStreamingOutputMonitor.main(inputSubject, natsUrl, clusterID)
  } else {
    val properties = new Properties()
    //@see https://github.com/tyagihas/java_nats/blob/master/src/main/java/org/nats/Connection.java
    properties.put("servers", natsUrl)
    val conn = Conn.connect(properties)
  
    if (args.length > 1) {// TEST mode
			// TODO Consider Integers & Floats
      val espectedValue = args(1).toInt
      println("Is especting a value equals to " + espectedValue)
      
      var iterations = 3
      conn.subscribe(inputSubject, (msg: Msg) => {
        println("Received message: " + msg.body)
        iterations -= 1
        if (iterations <= 0) {
          val receivedValue = msg.body.toInt
          if (receivedValue == espectedValue) { // "Tests passed!"
            System.exit(0)
          } else { // "Tests failed!"
            System.exit(1)
          }
        }
      })          
    } else { // REGULAR mode
      conn.subscribe(inputSubject, (msg: MsgB) => {
        val f = ByteBuffer.wrap(msg.body)
        if (msg.subject.contains("max")) {         
          println(s"Received message: (${msg.subject}, ${f.getFloat()})")
        } else {
           println(s"Received message: (${msg.subject}, ${f.getInt()})")
        }        
      })    
    }
  }
}