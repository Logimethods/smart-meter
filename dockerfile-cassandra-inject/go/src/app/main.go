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
//    if err := session.Query(`INSERT INTO messages (subject, message) VALUES (?, ?)`,
//        "subject1", "First message").Exec(); err != nil {
//        log.Print(err)
//    }
	
	// Simple Async Subscriber
	nc.Subscribe(nats_subject, func(m *nats.Msg) {
//	    fmt.Printf("Received a message: %s\n", string(m.Data))
		fmt.Println()
		
		query := "INSERT INTO raw_voltage_data (" +
				"line, transformer, usagePoint, year, month, day, hour, minute, day_of_week, voltage)" +
				" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
		
		// https://www.dotnetperls.com/split-go
		subjects := strings.Split(m.Subject, ".")
		// smartmeter.voltage.data.3.3.2	 | (2016-11-16T20:05:04,116.366646)
		len := len(subjects)		
		
		// http://stackoverflow.com/questions/30299649/golang-converting-string-to-specific-type-of-int-int8-int16-int32-int64
		line, _ := strconv.ParseInt(subjects[len -3], 10, 8) // tinyint,    // 8-bit signed int 
		fmt.Printf("line: %s\n", line)
		transformer, _ := strconv.ParseInt(subjects[len -2], 10, 32)	//  int, // 32-bit signed int
		fmt.Printf("transformer: %s\n", transformer)
		usagePoint, _ := strconv.ParseInt(subjects[len -1], 10, 32)	//  int,
		fmt.Printf("usagePoint: %s\n", usagePoint)

		fmt.Println("3 Data", m.Data)
		
		// http://stackoverflow.com/questions/11924196/convert-between-slices-of-different-types
//	    voltage := (*(*[4]int8)(unsafe.Pointer(&floatBytes[0])))[:]

//		fmt.Println("floatBytes", floatBytes)
//		voltage := Float32frombytes(floatBytes)	//  float,
//		fmt.Println("voltage", voltage)
		
		// https://blog.golang.org/go-slices-usage-and-internals
		//  val buffer = ByteBuffer.allocate(8+4);
	    //	buffer.putLong(date.atOffset(ZoneOffset.MIN).toEpochSecond())
	    //	buffer.putFloat(value)
		
		longBytes := m.Data[:8]
		epoch := int64(binary.BigEndian.Uint64(longBytes))
		fmt.Printf("longBytes=%v epoch=%v\n", longBytes, epoch)
		date := time.Unix(epoch, 0)
		fmt.Println(date)
		
		// https://golang.org/pkg/time/#Time
		year, month, day := date.Date()			
		fmt.Printf("year, month, day: %d, %d, %d\n", year, month, day)
		hour, minute, _ := date.Clock() 
		day_of_week := date.Weekday()
		fmt.Printf("hour, minute, day_of_week: %d, %d, %d\n", hour, minute, day_of_week)

		floatBytes := m.Data[8:]
		// http://stackoverflow.com/questions/22491876/convert-byte-array-uint8-to-float64-in-golang
		voltage := math.Float32frombits(binary.BigEndian.Uint32(floatBytes))
	    fmt.Printf("floatBytes=%v voltage=%v\n", floatBytes, voltage)
	    
	    // insert a message into Cassandra
	    if err := session.Query(query,
	        int8(line), int32(transformer), int32(usagePoint), int16(year), int8(month), int8(day), 
		    int8(hour), int8(minute), int8(day_of_week), voltage).Exec(); err != nil {
	        log.Print(err)
	    }
	})
	
	fmt.Println("Ready to store NATS messages into CASSANDRA")

	for {
	}
}