// MIT License
//
// Copyright (c) 2016-2017 Logimethods
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package main

import (
  "os"
  "log"
  "fmt"
  "time"
  "strings"

//  "bytes"
    "encoding/binary"
    "math"
    "strconv"
//    "unsafe"

  "github.com/nats-io/go-nats"
  "github.com/gocql/gocql"
)

const query = "INSERT INTO raw_data (" +
        "line, transformer, usagePoint, year, month, day, hour, minute, day_of_week, voltage, demand, " +
        "val3, val4, val5, val6, val7, val8, val9, val10, val11, val12) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

// http://docs.datastax.com/en/cql/3.3/cql/cql_using/useCounters.html
const increment = "UPDATE raw_data_count SET count = count + 1 WHERE slot = ?"

var Session *gocql.Session
var Cluster *gocql.ClusterConfig

func main() {
  log.Print("Welcome to the NATS to Cassandra Bridge")

  log.Print("The time is", time.Now())

  nats_uri := os.Getenv("NATS_URI")
  nc, _ := nats.Connect(nats_uri)

  log.Print("Subscribed to NATS: ", nats_uri)

  nats_subject := os.Getenv("NATS_SUBJECT")
  log.Print("NATS Subject: ", nats_subject)

  // CASSANDRA
  cassandra_url := os.Getenv("CASSANDRA_URL")
  log.Print("Cassandra URL: ", cassandra_url)

  // LOG LEVEL
  log_level := os.Getenv("LOG_LEVEL")
  log.Print("LOG LEVEL: ", log_level)

  // TASK_SLOT
  task_slot := os.Getenv("TASK_SLOT")
  log.Print("TASK SLOT: ", task_slot)

  // connect to the Cluster
  Cluster = gocql.NewCluster(cassandra_url)
  Cluster.Keyspace = "smartmeter"

  cluster_consistency := os.Getenv("CASSANDRA_INJECT_CONSISTENCY")
  var err error
  Cluster.Consistency, err = gocql.ParseConsistency(cluster_consistency)
  if (err == nil) {
    log.Print("CONSISTENCY: ", Cluster.Consistency)
  } else {
    log.Panicf("Unable to parse CASSANDRA_INJECT_CONSISTENCY through gocql.ParseConsistency(%s)", cluster_consistency)
  }

  cluster_timeout := os.Getenv("CASSANDRA_TIMEOUT")
  if (cluster_timeout != "") {
    timeout, err := strconv.Atoi(cluster_timeout)
    if (err == nil) {
      Cluster.Timeout = time.Duration(timeout) * time.Millisecond
      log.Print("(Provided) CASSANDRA_TIMEOUT: ", Cluster.Timeout)
    } else {
      log.Panicf("Unable to parse CASSANDRA_TIMEOUT (%s) into int64", cluster_timeout)
    }
  } else {
    log.Print("(Default) CASSANDRA_TIMEOUT: ", Cluster.Timeout)
  }

  createSession()
  defer Session.Close()

  log.Print("Connected to Cassandra")

  // Simple Async Subscriber
  nc.QueueSubscribe(nats_subject, "cassandra_inject", func(m *nats.Msg) {
    go insertIntoCassandra(m, task_slot, log_level)
  })

  log.Print("Ready to store NATS messages into CASSANDRA")

  // To keep the App alive
  for {
    time.Sleep(30 * time.Second)
    // log.Print(time.Now())
  }

}

func createSession() {
  log.Print("Cluster.CreateSession()")
  var err error
  Session, err = Cluster.CreateSession()
  if (err != nil) {
    log.Fatalf("Could not connect to Cassandra Cluster %s", err)
  }
}

func insertIntoCassandra(m *nats.Msg, task_slot string, log_level string) {
  /*** Point ***/

  // https://www.dotnetperls.com/split-go
  subjects := strings.Split(m.Subject, ".")
  // smartmeter.voltage.data.3.3.2   | (2016-11-16T20:05:04,116.366646)
  len := len(subjects)

  // http://stackoverflow.com/questions/30299649/golang-converting-string-to-specific-type-of-int-int8-int16-int32-int64
  line, _ := strconv.ParseInt(subjects[len -3], 10, 8) // tinyint,    // 8-bit signed int
  transformer, _ := strconv.ParseInt(subjects[len -2], 10, 32)  //  int, // 32-bit signed int
  usagePoint, _ := strconv.ParseInt(subjects[len -1], 10, 32)  //  int,

  /*** Date ***/

  longBytes := m.Data[:8]
  // http://stackoverflow.com/questions/22491876/convert-byte-array-uint8-to-float64-in-golang
  epoch := int64(binary.BigEndian.Uint64(longBytes))
  date := time.Unix(epoch, 0)

  // https://golang.org/pkg/time/#Time
  year, month, day := date.Date()
  hour, minute, _ := date.Clock()
  day_of_week := date.Weekday()

  /*** Voltage ***/

  voltageFloatBytes := m.Data[8:12]
  // http://stackoverflow.com/questions/22491876/convert-byte-array-uint8-to-float64-in-golang
  voltage := math.Float32frombits(binary.BigEndian.Uint32(voltageFloatBytes))

  /*** Demand ***/

  demandFloatBytes := m.Data[12:16]
  // http://stackoverflow.com/questions/22491876/convert-byte-array-uint8-to-float64-in-golang
  demand := math.Float32frombits(binary.BigEndian.Uint32(demandFloatBytes))

  /*** Remaining Values ***/

  var values [10]float32
  for i := 0; i < 10; i++ {
    values[i] = math.Float32frombits(binary.BigEndian.Uint32(m.Data[(16+4*i):(20+4*i)]))
  }

  // fmt.Print(".")

  if (log_level == "TRACE") {
    s := fmt.Sprintf("- v: %d", voltage)
    log.Print(s)
  }

  if (Session == nil || Session.Closed()) {
    createSession()
  }

  /** insert the Data into Cassandra **/

  if err := Session.Query(query,
      int8(line), int32(transformer), int32(usagePoint), int16(year), int8(month), int8(day),
      int8(hour), int8(minute), int8(day_of_week), voltage, demand,
      values[0], values[1], values[2], values[3], values[4],
      values[5], values[6], values[7], values[8], values[9]).Exec(); err != nil {
      log.Panic(err)
  } else {
    if err := Session.Query(increment, task_slot).Exec(); err != nil {
        log.Panic(err)
    }
  }
}
