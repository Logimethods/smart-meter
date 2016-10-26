package com.logimethods.nats.connector.spark.monitor;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import io.nats.stan.*;
import io.nats.stan.Message;

public class NatsStreamingOutputMonitor {

	public static void main(String inputSubject, String natsUrl, String clusterID) throws InterruptedException, IOException, TimeoutException {
		final ConnectionFactory connectionFactory = new ConnectionFactory(clusterID, "NatsStreamingOutputMonitor_CLIENT");
		connectionFactory.setNatsUrl(natsUrl);
		final Connection connection = connectionFactory.createConnection();
		connection.subscribe(inputSubject, "NatsStreamingOutputMonitor_QUEUE", new MessageHandler() {
//			@Override
			public void onMessage(Message m) {
				String s = new String(m.getData());
				System.out.println("Received message: " + s);
			}
		}, null);
	}

}
