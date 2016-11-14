package main

import (
	"fmt"
	"time"
	
	"github.com/nats-io/nats"
)

func main() {
	fmt.Println("Welcome to the playground!")

	fmt.Println("The time is", time.Now())
	
	nc, _ := nats.Connect(nats.DefaultURL)
	
	// Simple Async Subscriber
	nc.Subscribe("foo", func(m *nats.Msg) {
	    fmt.Printf("Received a message: %s\n", string(m.Data))
	})
}