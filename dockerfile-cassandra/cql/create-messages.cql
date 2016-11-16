/*
 * Copyright 2016 Logimethods - Laurent Magnin
 */

//DROP KEYSPACE IF EXISTS power_data;
//"Attempting to create an already existing keyspace will return an error unless the IF NOT EXISTS option is used." -> We don't want to erase existing data
CREATE KEYSPACE IF NOT EXISTS smartmeter WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE smartmeter;

CREATE TABLE IF NOT EXISTS messages (
	subject text,
	message text,
	PRIMARY KEY (subject)
);