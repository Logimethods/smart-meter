/*
 * Copyright 2016 Logimethods - Laurent Magnin
 */

//DROP KEYSPACE IF EXISTS power_data;
//"Attempting to create an already existing keyspace will return an error unless the IF NOT EXISTS option is used." -> We don't want to erase existing data
CREATE KEYSPACE smartmeter WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE smartmeter;

CREATE TABLE messages (
	subject text,
	message text,
	PRIMARY KEY (subject)
);


CREATE TABLE usage_point (
      usagePointPK int, // 32-bit signed int
      year smallint,    // 16-bit signed int                   
      month tinyint,    // 8-bit signed int
      day tinyint,
      line tinyint, 
      transformer int, 
      usagePoint int,
      PRIMARY KEY ((usagePointPK), year, month, day)
) WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC);

// See http://www.datastax.com/dev/blog/datetieredcompactionstrategy
// See http://www.datastax.com/dev/blog/dtcs-notes-from-the-field
ALTER TABLE usage_point WITH compaction = {
	'class': 'DateTieredCompactionStrategy', 
	'base_time_seconds':'864000', // Ten days
	'max_sstable_age_days':'730'}; // Two years


CREATE TABLE raw_usage_data (
      usagePointPK int, 
      year smallint,                     
      month tinyint, 
      day tinyint, 
      hour tinyint, 
      minute tinyint, 
      day_of_week tinyint,
      demand float, 
      voltage float,
      PRIMARY KEY ((usagePointPK), year, month, day, hour, minute)
) WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC, minute DESC);

// See http://www.datastax.com/dev/blog/datetieredcompactionstrategy
// See http://www.datastax.com/dev/blog/dtcs-notes-from-the-field
ALTER TABLE raw_usage_data WITH compaction = {
	'class': 'DateTieredCompactionStrategy', 
	'base_time_seconds':'86400', // One day
	'max_sstable_age_days':'730'}; // Two years


/*
  Sum of all demand for one day of week and one usage point
*/
CREATE TABLE daily_aggregate_demand (
      usagePointPK int, 
      year smallint,                     
      month tinyint, 
      day_of_week tinyint,
      demand counter, 
      PRIMARY KEY ((usagePointPK), year, month, day_of_week)
) WITH CLUSTERING ORDER BY (year DESC, month DESC, day_of_week DESC);


/*
  Sum of all demand for one shift and one usage point
*/
CREATE TABLE shift_aggregate_demand (
      usagePointPK int, 
      year smallint,                     
      month tinyint, 
      shift tinyint,
      demand counter, 
      PRIMARY KEY ((usagePointPK), year, month, shift)
) WITH CLUSTERING ORDER BY (year DESC, month DESC, shift DESC);
