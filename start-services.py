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
	
def create_service(name, replicas, postfix)
	subprocess.run(["bash", "docker_service.sh", "-r", str(replicas), "create_service_" + name, postfix])

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

client.networks.create("smart-meter-net", driver="overlay")





