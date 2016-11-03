/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.smartmeter.inject

import akka.actor.{ActorRef, Props}
import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder

import com.logimethods.connector.gatling.to_nats._

import scala.concurrent.duration._
import java.util.Properties
import io.nats.client.Constants.PROP_URL

import com.logimethods.smartmeter.generate._

class NatsInjection extends Simulation {
  
  val properties = new Properties()
  val natsUrl = System.getenv("NATS_URI")
  properties.setProperty(io.nats.client.Constants.PROP_URL, natsUrl)
  println("System properties: " + System.getenv())
  
  var subject = System.getenv("GATLING_TO_NATS_SUBJECT")
  if (subject == null) {
    println("No Subject has been defined through the 'GATLING_TO_NATS_SUBJECT' Environment Variable!!!")
  } else {
    println("Will emit messages to " + subject)
    val natsProtocol = NatsProtocol(properties, subject)
    
    val natsScn = scenario("NATS call").exec(NatsBuilder(new ConsumerInterpolatedVoltageProvider()))
   
    setUp(
      natsScn.inject(constantUsersPerSec(27) during (1 minute))
    ).protocols(natsProtocol)
  }
}