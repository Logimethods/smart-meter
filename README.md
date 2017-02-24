# smart-meter
To demonstrate a Smart Meter Big Data Application.

![SmartMeter.png](SmartMeter.png "SmartMeter Architecture")

## Python CLI
See [start-services.py](start-services.py)
```
> python3 -i ./start-services.py
>>> run_inject()
>>> run_app_batch()
>>> exit()
```

In parallel, you can play with the number of injectors:
```
> docker service scale inject=2
> docker service scale inject=1
```

## CQLSH (Cassandra CLI)
To access to the RAW Voltage Data:
```
> ./cqlsh.sh
Connected to Smartmeter Cluster at 127.0.0.1:9042.
[cqlsh 5.0.1 | Cassandra 3.5 | CQL spec 3.4.0 | Native protocol v4]
Use HELP for help.
cqlsh> select * from smartmeter.raw_voltage_data limit 2;

 line | transformer | usagepoint | year | month | day | hour | minute | day_of_week | voltage
------+-------------+------------+------+-------+-----+------+--------+-------------+-----------
    3 |           2 |          2 | 2016 |    12 |  24 |    3 |     16 |           6 | 121.15018
    3 |           2 |          2 | 2016 |    12 |  24 |    3 |      1 |           6 | 121.66259
```

## Grafana metrics
[http://localhost/dashboard/db/gatling-max-voltage](http://localhost/dashboard/db/gatling-max-voltage)

![gatling-max-voltage_screenshot.png](images/gatling-max-voltage_screenshot.png "Gatling-max-voltage Screenshot")

## Local DEV
```
> ./build-local.sh
> ./stop.sh
> python3 -i start-services.py "local"
Images will be postfixed by -local
>>>
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

* You might use the SQL syntax, such as `select * from smartmeter.raw_voltage_data limit 10;`
* Et Voilà!

![from_Cassandra_2_Excel.png](excel/from_Cassandra_2_Excel.png)
