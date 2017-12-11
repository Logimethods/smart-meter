/*
 * Copyright 2016-2017 Logimethods - Laurent Magnin
 */

//"Attempting to create an already existing keyspace will return an error unless the IF NOT EXISTS option is used." -> We don't want to erase existing data
CREATE KEYSPACE IF NOT EXISTS smartmeter WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE smartmeter;

CREATE TABLE IF NOT EXISTS raw_data (
      line tinyint,    // 8-bit signed int
      transformer int, // 32-bit signed int
      usagePoint int,
      year smallint,    // 16-bit signed int
      month tinyint,
      day tinyint,
      hour tinyint,
      minute tinyint,
      day_of_week tinyint,
      voltage float,
      PRIMARY KEY ((line, transformer, usagePoint), year, month, day, hour, minute)
) WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC, minute DESC);

// http://docs.datastax.com/en/cql/3.3/cql/cql_using/useCounters.html
CREATE TABLE IF NOT EXISTS raw_data_count (
      slot ascii PRIMARY KEY,
      count counter
);

CREATE TABLE IF NOT EXISTS voltage_alerts (
      line tinyint,    // 8-bit signed int
      transformer int, // 32-bit signed int
      year smallint,    // 16-bit signed int
      month tinyint,
      day tinyint,
      voltage_avg float,
      PRIMARY KEY ((line, transformer), year, month, day)
) WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC);

CREATE TABLE IF NOT EXISTS max_voltage (
      epoch bigint,
      voltage float,
      PRIMARY KEY (epoch)
);

CREATE TABLE IF NOT EXISTS temperature (
      epoch bigint,
      temperature float,
      PRIMARY KEY (epoch)
);

CREATE TABLE IF NOT EXISTS avg_voltage_by_transformer (
      line tinyint,    // 8-bit signed int
      transformer int, // 32-bit signed int
      year smallint,    // 16-bit signed int
      month tinyint,
      day tinyint,
      voltage_avg float,
      PRIMARY KEY ((line, transformer), year, month, day)
) WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC);
