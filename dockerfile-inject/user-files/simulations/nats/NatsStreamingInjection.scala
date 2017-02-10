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

class NatsStreamingInjection extends Simulation {
  
  val natsUrl = System.getenv("NATS_URI")
  val clusterID = System.getenv("NATS_CLUSTER_ID")
  
  var subject = System.getenv("GATLING_TO_NATS_SUBJECT")
  if (subject == null) {
    println("No Subject has been defined through the 'GATLING_TO_NATS_SUBJECT' Environment Variable!!!")
  } else {
    println("Will emit messages to " + subject)
    val natsProtocol = NatsStreamingProtocol(natsUrl, clusterID, subject)
    
    val usersPerSec = System.getenv("GATLING_USERS_PER_SEC").toDouble
    val duration = System.getenv("GATLING_DURATION").toInt
    val slot = System.getenv("TASK_SLOT").toInt
	    
    val natsScn = scenario("smartmeter").exec(NatsStreamingBuilder(new ConsumerInterpolatedVoltageProvider(slot, usersPerSec)))
   
    setUp(
      natsScn.inject(constantUsersPerSec(usersPerSec) during (duration minute))
    ).protocols(natsProtocol)
  }
}