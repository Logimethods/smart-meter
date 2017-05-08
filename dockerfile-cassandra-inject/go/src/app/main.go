package main

import (
	"os"
	"log"
	"fmt"
	"time"
	"strings"

//	"bytes"
    "encoding/binary"
    "math"
    "strconv"
//    "unsafe"

	"github.com/nats-io/nats"
	"github.com/gocql/gocql"
)

const query = "INSERT INTO raw_data (" +
			  "line, transformer, usagePoint, year, month, day, hour, minute, day_of_week, voltage, demand, " +
			  "val3, val4, val5, val6, val7, val8, val9, val10, val11, val12) " +
			  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			  
func insertIntoCassandra(session *gocql.Session, m *nats.Msg, log_level string) {
	/*** Point ***/

	// https://www.dotnetperls.com/split-go
	subjects := strings.Split(m.Subject, ".")
	// smartmeter.voltage.data.3.3.2	 | (2016-11-16T20:05:04,116.366646)
	len := len(subjects)

	// http://stackoverflow.com/questions/30299649/golang-converting-string-to-specific-type-of-int-int8-int16-int32-int64
	line, _ := strconv.ParseInt(subjects[len -3], 10, 8) // tinyint,    // 8-bit signed int
	transformer, _ := strconv.ParseInt(subjects[len -2], 10, 32)	//  int, // 32-bit signed int
	usagePoint, _ := strconv.ParseInt(subjects[len -1], 10, 32)	//  int,

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
		fmt.Println(s)
	}

    /** insert the Data into Cassandra **/

    if err := session.Query(query,
        int8(line), int32(transformer), int32(usagePoint), int16(year), int8(month), int8(day),
	    int8(hour), int8(minute), int8(day_of_week), voltage, demand,
	    values[0], values[1], values[2], values[3], values[4], 
	    values[5], values[6], values[7], values[8], values[9]).Exec(); err != nil {
        log.Print(err)
    }
}

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
	fmt.Println("Cassandra URL: ", cassandra_url)

	// LOG LEVEL
	log_level := os.Getenv("LOG_LEVEL")
	fmt.Println("LOG LEVEL: ", log_level)

	// connect to the cluster
	cluster := gocql.NewCluster(cassandra_url)
	cluster.Keyspace = "smartmeter"

	cluster_consistency := os.Getenv("CASSANDRA_INJECT_CONSISTENCY")
	cluster.Consistency = gocql.ParseConsistency(cluster_consistency)
	fmt.Println("CONSISTENCY: ", cluster.Consistency)

	session, _ := cluster.CreateSession()
	defer session.Close()

	fmt.Println("Connected to Cassandra")

    // insert a message into Cassandra
//    if err := session.Query(`INSERT INTO messages (subject, message) VALUES (?, ?)`,
//        "subject1", "First message").Exec(); err != nil {
//        log.Print(err)
//    }

	// Simple Async Subscriber
	nc.QueueSubscribe(nats_subject, "cassandra_inject", func(m *nats.Msg) {
		go insertIntoCassandra(session, m, log_level)
	})

	fmt.Println("Ready to store NATS messages into CASSANDRA")

	for {
		time.Sleep(30 * time.Second)
		// fmt.Println(time.Now())
	}
}
