/*
 * Copyright 2016 Logimethods - Laurent Magnin
 */

//DROP KEYSPACE IF EXISTS power_data;
//"Attempting to create an already existing keyspace will return an error unless the IF NOT EXISTS option is used." -> We don't want to erase existing data
CREATE KEYSPACE IF NOT EXISTS smartmeter WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE smartmeter;

CREATE TABLE IF NOT EXISTS raw_voltage_data (
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
