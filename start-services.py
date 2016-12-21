import subprocess
import docker
client = docker.from_env()

if (len(sys.argv) > 1):
	postfix = sys.argv[0]
	print("Images will be postfixed by " + postfix)
else:
	postfix = ""

def update_replicas(service, replicas):
	param = service.name + "=" + str(replicas)
	subprocess.run(["docker", "service", "scale", param])
	
def create_service(name, replicas, postfix):
	subprocess.run(["bash", "docker_service.sh", "-r", str(replicas), "create_service_" + name, postfix])

def create(name):
	subprocess.run(["bash", "docker_service.sh", "create_" + name])

def get_service(name):
	services = client.services.list()
	for service in services:
		if service.name == name:
			return service
	return None

def create_or_update_service(name, replicas, postfix):
	service = get_service(name)
	if service is not None:
		update_replicas(service, replicas)
	else:
		create_service(name, replicas, postfix)

def create_network():
	client.networks.create("smart-meter-net", driver="overlay")

def create_scenario(steps):
	for step in steps:
		if step[0] == "create_service" :
			create_or_update_service(step[1], step[2], postfix)
		else:
			create(step[1])

create_network = ["create", "network"]
create_service_cassandra = ["create_service", "cassandra", 1]
create_service_spark = ["create_service", "spark", 2]
create_service_nats = ["create_service", "nats", 1]
create_service_app_streaming = ["create_service", "app-streaming", 1]
create_service_monitor = ["create_service", "monitor", 1]
create_service_reporter = ["create_service", "reporter", 1]
create_cassandra_tables = ["create", "cassandra_tables", 1]
create_service_cassandra_inject = ["create_service", "cassandra-inject", 1]
create_service_inject = ["create_service", "inject", 1]

all_steps = [
	create_network,
	create_service_cassandra,
	create_service_spark,
	create_service_nats,
	create_service_app_streaming,
	create_service_monitor,
	create_service_reporter,
	create_cassandra_tables,
	create_service_cassandra_inject,
	create_service_inject
	]

def run_all_steps():
	create_scenario(all_steps)
