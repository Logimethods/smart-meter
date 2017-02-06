# smart-meter
To demonstrate a Smart Meter Big Data Application.

![SmartMeter.png](SmartMeter.png "SmartMeter Architecture")

## Python CLI
See [start-services.py](start-services.py)
```
> python3 -i start-services.py
>>> run_inject_raw_data_into_cassandra()
>>> run_app_batch()
...
>>> run_or_kill_scenario([create_service_cassandra])
```

## Excel

### Install the ODBC Driver

* Get the Driver from http://www.simba.com/drivers/cassandra-odbc-jdbc/
* Follow the Installation Instructions (on MacOS, don't forget first to install [iODBC](http://www.iodbc.org/))
* Save the Licence file you received by Mail (`SimbaApacheCassandraODBCDriver.lic`) into the right location

### Create a SDN File

* Define a SDN file, such as [excel/cassandra.dsn](excel/cassandra.dsn)
* You could load & test it directly through the iODBC Administrator App:
![iODBC_test_sdn_file.png](excel/iODBC_test_sdn_file.png)

### Connect to the External Data from Excel using the `iODBC Data Source Chooser` (File DSN)

* You might use the SQL syntax, such as `select * from raw_voltage_data limit 10;`
* Et Voilà!

![from_Cassandra_2_Excel.png](excel/from_Cassandra_2_Excel.png)
