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

client.networks.create("smart-meter-net", driver="overlay")


services = client.services.list()


