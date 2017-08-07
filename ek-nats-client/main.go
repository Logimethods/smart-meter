package main

import (
	"fmt"
	"os"
  "math"
  "time"
  "strings"
  "github.com/nats-io/go-nats"
)

func main() {
  user, _ := os.LookupEnv("NATS_USERNAME")
  fmt.Println("NATS_USERNAME=",user[:2],"...")
  pwd, _ := os.LookupEnv("NATS_PASSWORD")
  fmt.Println("NATS_PASSWORD=",pwd[:2],"...")
  nats_name, _ := os.LookupEnv("NATS_NAME")
  fmt.Println("NATS_NAME=",nats_name,"=")
  nar_url := "nats://"+nats_name+":4222"
  fmt.Println("nar_url=",nar_url)

  nc, err := nats.Connect(nar_url, nats.UserInfo(user, pwd))
  if (err != nil) {
    fmt.Println("error ", err.Error())
  }
  for ((err != nil) && strings.Contains(err.Error(), "no servers available for connection")) {
    nc, err = nats.Connect(nar_url, nats.UserInfo(user, pwd))
  }
  if (err == nil) {
    defer nc.Close()
    fmt.Println("Listening")

    nats_subject := os.Getenv("NATS_SUBJECT")
  	fmt.Println("NATS Subject: ", nats_subject)

    // Simple Async Subscriber
  	nc.QueueSubscribe(nats_subject, "cassandra_inject", func(m *nats.Msg) {
  		fmt.Println(">", m)
  	})
  } else {
    fmt.Println(err)
  }

  // Wait (almost) forever
  // https://blog.sgmansfield.com/2016/06/how-to-block-forever-in-go/
  <-time.After(time.Duration(math.MaxInt64))
}
