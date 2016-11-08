/*******************************************************************************
 * Copyright (c) 2016 Logimethods
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package com.logimethods.nats.connector.spark.monitor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import io.nats.stan.*;
import io.nats.stan.Message;

public class NatsStreamingOutputMonitor {

	public static void main(String inputSubject, String natsUrl, String clusterID) throws InterruptedException, IOException, TimeoutException {
		final ConnectionFactory connectionFactory = new ConnectionFactory(clusterID, "NatsStreamingOutputMonitor_CLIENT");
		connectionFactory.setNatsUrl(natsUrl);
		final Connection connection = connectionFactory.createConnection();
		connection.subscribe(inputSubject, "NatsStreamingOutputMonitor_QUEUE", new MessageHandler() {
			@Override
			public void onMessage(Message m) {
				// TODO Consider Integers
				final Float f = ByteBuffer.wrap(m.getData()).getFloat();
				System.out.println("Received message: (" + m.getSubject() + ", " + f + ')');
			}
		}, null);
	}

}
