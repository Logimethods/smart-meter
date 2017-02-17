docker exec -it cassandra_main nodetool cfstats smartmeter.raw_voltage_data
echo "Local write count"
docker exec -it cassandra_main nodetool cfstats smartmeter.raw_voltage_data | grep "Local write count" | rev | cut -d ' ' -f 1 | rev
