from subprocess import Popen, PIPE
import docker
client = docker.from_env()

if (len(sys.argv) > 1):
	postfix = sys.argv[0]
	print("Images will be postfixed by " + postfix)
else:
	postfix = ""

NATS_USERNAME="smartmeter"
NATS_PASSWORD="xyz1234"

client.networks.create("smart-meter-net", driver="overlay")

from subprocess import run
subprocess.run(["bash", "docker_service", "create_service_cassandra", "-local"])

