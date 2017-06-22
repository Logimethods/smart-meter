#!/usr/bin/python

import sys
import subprocess
import docker

global_params = []

## location

if (len(sys.argv) > 1):
	location = sys.argv[1]
else:
	location = "local"

global_params += ["-l", location]
print("Will run in " + location + " location.")

if (location == "remote"):
	client = docker.DockerClient(base_url='tcp://localhost:2374')
	print("Remote Docker Client")
else:
	client = docker.from_env()
	print("Local Docker Client")

## Cluster Mode

if (len(sys.argv) > 2):
	cluster_mode = sys.argv[2]
else:
	cluster_mode = "single"

global_params += ["-m", cluster_mode]
print("Images will run in " + cluster_mode + " mode.")

## Postfix

if (len(sys.argv) > 3):
	postfix = "-" + sys.argv[3]
	global_params += ["-p", postfix]
	print("Images will be postfixed by " + postfix)
else:
	postfix = ""

#######

def update_replicas(service, replicas):
	param = service.name + "=" + str(replicas)
	# subprocess.run(["docker", "service", "scale", param])
	subprocess.run(["bash", "start-services_exec.sh", "-r", str(replicas)] + global_params + ["scale_service", param])

def run_service(name, replicas, postfix):
	if replicas > 0:
		subprocess.run(["bash", "start-services_exec.sh", "-r", str(replicas)] + global_params + ["create_service_" + name])

def call(type, name, parameters):
	subprocess.run(["bash", "start-services_exec.sh"] + global_params + [type + "_" + name] + parameters)

def get_service(name):
	services = client.services.list()
	for service in services:
		if service.name == name:
			return service
	return None

def create_service(name, replicas, postfix):
	service = get_service(name)
	if service is not None:
		update_replicas(service, replicas)
	else:
		run_service(name, replicas, postfix)

def create_service_telegraf(name, postfix):
	subprocess.run(["bash", "start-services_exec.sh"] + global_params + ["create_service_telegraf", name])

def rm_service(name, postfix):
	# subprocess.run(["docker", "service", "rm", name])
	subprocess.run(["bash", "start-services_exec.sh"] + global_params + ["rm_service", name])

def create_network():
	client.networks.create("smartmeter", driver="overlay")

## RUN SCENARIO ##

def run(steps):
	if not isinstance(steps[0], list):
		steps = [steps]
	for step in steps:
		print()
		print(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
		print(step)
		if step[0] == "create_service" :
			create_service(step[1], step[2], postfix)
		elif step[0] == "rm_service" :
			rm_service(step[1], postfix)
		elif step[0] == "create_service_telegraf" :
			create_service_telegraf(step[1], postfix)
		else:
			call(step[0], step[1], step[2:])

def run_or_kill(steps):
	if not isinstance(steps[0], list):
		steps = [steps]
	# Collect all existing services names
	all_remaining_services = []
	for step in all_steps:
		if step[0] == "create_service" :
			all_remaining_services.append(step[1])
	# Remove all requested services
	for step in steps:
		if (step[0] == "create_service") and (step[2] > 0):
			all_remaining_services.remove(step[1])
	#
	print("All of those services will be deleted: " + str(all_remaining_services))
	for name in all_remaining_services:
		rm_service(name, postfix)
	# Finaly, run the requested scenario
	run(steps)

## PREDEFINED STEPS ##

create_network = ["create", "network"]
create_service_cassandra = ["create_service", "cassandra", 1]
create_service_spark_master = ["create_service", "spark-master", 1]
create_service_spark_slave = ["create_service", "spark-slave", 1]
create_service_nats = ["create_service", "nats", 1]
create_service_app_streaming = ["create_service", "app_streaming", 1]
create_service_prediction_trainer = ["create_service", "prediction_trainer", 1]
create_service_prediction_oracle = ["create_service", "prediction_oracle", 1]
create_service_monitor = ["create_service", "monitor", 1]
create_service_reporter = ["create_service", "reporter", 1]
create_cassandra_tables = ["call", "cassandra_cql", "/cql/create-timeseries.cql"]
create_service_cassandra_inject = ["create_service", "cassandra-inject", 1]
create_service_inject = ["create_service", "inject", 1]
create_service_app_batch = ["create_service", "app-batch", 1]

stop_service_cassandra = ["create_service", "cassandra", 0]
stop_service_spark_master = ["create_service", "spark-master", 0]
stop_service_spark_slave = ["create_service", "spark-slave", 0]
stop_service_nats = ["create_service", "nats", 0]
stop_service_app_streaming = ["create_service", "app_streaming", 0]
stop_service_prediction_trainer = ["create_service", "prediction_trainer", 0]
stop_service_prediction_oracle = ["create_service", "prediction_oracle", 0]
stop_service_monitor = ["create_service", "monitor", 0]
stop_service_reporter = ["create_service", "reporter", 0]
stop_service_cassandra_inject = ["create_service", "cassandra-inject", 0]
stop_service_inject = ["create_service", "inject", 0]
stop_service_app_batch = ["create_service", "app-batch", 0]

rm_service_cassandra = ["rm_service", "cassandra"]
rm_service_spark_master = ["rm_service", "spark-master"]
rm_service_spark_slave = ["rm_service", "spark-slave"]
rm_service_nats = ["rm_service", "nats"]
rm_service_app_streaming = ["rm_service", "app_streaming"]
rm_service_app_prediction = ["rm_service", "app_prediction"]
rm_service_monitor = ["rm_service", "monitor"]
rm_service_reporter = ["rm_service", "reporter"]
rm_service_cassandra_inject = ["rm_service", "cassandra-inject"]
rm_service_inject = ["rm_service", "inject"]
rm_service_app_batch = ["rm_service", "app-batch"]

run_metrics = ["run", "metrics"]
run_telegraf_max_voltage = ["run", "telegraf", "max_voltage"]

all_steps = [
	create_network,
	create_service_cassandra,
	create_service_spark_master,
	create_service_spark_slave,
	create_service_nats,
	create_service_app_streaming,
	create_service_prediction_trainer,
    create_service_prediction_oracle,
	create_service_monitor,
	create_service_reporter,
	create_cassandra_tables,
	create_service_cassandra_inject,
	create_service_inject,
	create_service_app_batch,
	run_metrics
	]

## PREDEFINED SCENARII ##

def stop_all():
	run(["stop", "all"])

def run_all_steps():
	run(all_steps)

def run_setup_cassandra():
	run([
		create_network,
###		["create_volume", "cassandra"],
		create_service_cassandra,
#		["wait", "service", "cassandra"],
		create_cassandra_tables,
		])

def run_inject():
	run([
		create_network,
		["create_service", "visualizer", 1],
		["create_service", "eureka", 1],
		run_metrics,
###		["create_volume", "cassandra"],
#		rm_service_inject,
#		["run_service", "telegraf_docker"],
###		create_service_spark_master,
###		["wait", "service", "spark-master"],
###		create_service_spark_slave,
###		["run", "spark_autoscaling"],
#		rm_service_cassandra_inject,
#		["build", "inject"],
		["create_service", "hadoop", 1],
		["create_service", "cassandra_single", 1],
		["create_service", "nats_single", 1],
#		["wait", "service", "cassandra"],
		["wait", "service", "nats"],
		create_cassandra_tables,
		create_service_cassandra_inject,
		create_service_app_streaming,
		create_service_prediction_trainer,
		["run", "telegraf", "max_voltage"],
		["run", "telegraf", "temperature"],
		["run", "telegraf", "prediction"],
##		["run", "telegraf", "cassandra"],
		["run", "telegraf", "docker"],
		["run", "telegraf", "cassandra_write_count"],
		["create_service_telegraf", "cassandra"],
		["run", "prometheus_nats_exporter"],
#		["run", "telegraf", "cassandra_count"],
#		["wait", "service", "cassandra-inject"],
		create_service_prediction_oracle,
		create_service_inject,
#		["run", "app_prediction"]
#		["run", "inject", "2"],
#		["logs", "service", "cassandra-inject-local"],
		])


def run_inject_eureka():
	run([
		create_network,
		["create_service", "eureka", 1],
		["create_service", "visualizer", 1],
		run_metrics,
		["create_service", "hadoop", 1],
		create_service_cassandra_inject,
		["create_service", "cassandra_single", 1],
		["create_service", "nats_single", 1],
		create_service_app_streaming,
		create_service_prediction_trainer,
		["run", "telegraf", "max_voltage"],
		["run", "telegraf", "temperature"],
		["run", "telegraf", "prediction"],
		["run", "telegraf", "docker"],
		["run", "telegraf", "cassandra_write_count"],
		["create_service_telegraf", "cassandra"],
		["run", "prometheus_nats_exporter"],
		create_service_prediction_oracle,
		create_service_inject,
		])

def run_inject_cluster():
	run([
		create_network,
		["create_service", "visualizer", 1],
		["create_service", "eureka", 1],
		run_metrics,
		["create_service", "hadoop", 1],
		["create_volume", "cassandra"],
		create_service_spark_master,
#		["wait", "service", "spark-master"],
		create_service_spark_slave,
		create_service_cassandra,
		create_service_nats,
#		["wait", "service", "nats"],
#		create_cassandra_tables,
		create_service_cassandra_inject,
		create_service_app_streaming,
		create_service_prediction_trainer,
		["run", "telegraf", "max_voltage"],
		["run", "telegraf", "temperature"],
		["run", "telegraf", "prediction"],
		["create_service_telegraf", "cassandra_write_count"],
		["create_service_telegraf", "cassandra"],
		["create_service", "prometheus_nats_exporter", 1],
		create_service_prediction_oracle,
		create_service_inject
		])

def monitor_cassandra():
	run_or_kill([
		create_network,
		["build", "cassandra"],
		run_metrics,
		create_service_cassandra,
#		["wait", "service", "cassandra"],
		["run", "telegraf", "cassandra"]
		])

def run_inject_raw_data_into_cassandra():
	run_or_kill([
		create_network,
		run_metrics,
		rm_service_inject,
		rm_service_cassandra_inject,
#		["build", "inject"],
		create_service_cassandra,
		create_service_nats,
		["wait", "service", "cassandra"],
		create_service_cassandra_inject,
		["wait", "service", "nats"],
		["wait", "service", "cassandra-inject"],
		create_service_inject,
#		["run", "inject", "2"],
		["logs", "service", "cassandra-inject-local"],
		])

def run_app_batch():
	run(["run", "app-batch"])

def run_full_app_batch():
	run([
		create_network,
		stop_service_app_batch,
#		["build", "app-batch"],
		create_service_cassandra,
		create_service_spark_master,
		["wait", "service", "spark-master"],
		create_service_spark_slave,
#		["wait", "service", "cassandra"],
		["run", "app-batch"]
#		["wait", "service", "app-batch"],
#		["logs", "service", "app-batch"]
		])
