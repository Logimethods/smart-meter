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
	fmt.Println("Welcome to the playground!")

	fmt.Println("The time is", time.Now())
	
	nc, _ := nats.Connect(nats.DefaultURL)
	
	fmt.Println("Subscribed to NATS")
	
	// CASSANDRA

	cluster_ip := os.Getenv("CASSANDRA_CLUSTER")
	fmt.Printf("cluster IP: ", cluster_ip)
	
    // connect to the cluster
    cluster := gocql.NewCluster(cluster_ip)
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
	nc.Subscribe(">", func(m *nats.Msg) {
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