package main

import (
	"os"
	"log"
	"fmt"
	"time"
	
	"github.com/nats-io/nats"
	"github.com/gocql/gocql"
)

func main() {
	fmt.Println("Welcome to the NATS to Cassandra Bridge")

	fmt.Println("The time is", time.Now())
	
	nats_uri := os.Getenv("NATS_URI")
	nc, _ := nats.Connect(nats_uri)
	
	fmt.Println("Subscribed to NATS: ", nats_uri)

	nats_subject := os.Getenv("NATS_SUBJECT")
	fmt.Println("NATS Subject: ", nats_subject)
	
	// CASSANDRA

	cassandra_url := os.Getenv("CASSANDRA_URL")
	fmt.Printf("Cassandra URL: ", cassandra_url)
	
    // connect to the cluster
    cluster := gocql.NewCluster(cassandra_url)
    cluster.Keyspace = "smartmeter"
    cluster.Consistency = gocql.Quorum
    session, _ := cluster.CreateSession()
    defer session.Close()
	
	fmt.Println("Connected to Cassandra")

    // insert a message into Cassandra
    if err := session.Query(`INSERT INTO messages (subject, message) VALUES (?, ?)`,
        "subject1", "First message").Exec(); err != nil {
        log.Print(err)
    }
	
	// Simple Async Subscriber
	nc.Subscribe(nats_subject, func(m *nats.Msg) {
	    fmt.Printf("Received a message: %s\n", string(m.Data))
	    
	    // insert a message into Cassandra
	    if err := session.Query(`INSERT INTO messages (subject, message) VALUES (?, ?)`,
	        m.Subject, string(m.Data)).Exec(); err != nil {
	        log.Print(err)
	    }
	})
	
	fmt.Println("Ready to store NATS messages into CASSANDRA")

	for {
	}
}