package main

import (
	"fmt"
	"os"
  "math"
  "time"
  "strings"
  "github.com/nats-io/nats"
)

func main() {
  fmt.Println("NATS_PASSWORD:",os.Getenv("NATS_PASSWORD"),":")

  user, _ := os.LookupEnv("NATS_USERNAME")
  fmt.Println("NATS_USERNAME=",user,"=")
  pwd, _ := os.LookupEnv("NATS_PASSWORD")
  fmt.Println("NATS_PASSWORD=",pwd,"=")

  nc, err := nats.Connect("nats://nats:4222", nats.UserInfo(user, pwd))
  if (err != nil) {
    fmt.Println("error ", err.Error())
  }
  for ((err != nil) && strings.Contains(err.Error(), "no servers available for connection")) {
    nc, err = nats.Connect("nats://nats:4222", nats.UserInfo(user, pwd))
  }
  if (err == nil) {
    defer nc.Close()
    nc.Publish("foo", []byte("Hello World"))
    fmt.Println("Value published")
  } else {
    fmt.Println(err)
  }

  // Wait forever
  // https://blog.sgmansfield.com/2016/06/how-to-block-forever-in-go/
  <-time.After(time.Duration(math.MaxInt64))
}
