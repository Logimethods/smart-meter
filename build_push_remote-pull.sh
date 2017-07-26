#!/bin/bash

set -e

clear
echo "-----------------------------------------"
echo "build_dockerfile_inject $extension"
#build_dockerfile_inject ${extension}
docker push logimethods/smart-meter:inject${extension}
docker -H localhost:2374 pull logimethods/smart-meter:inject${extension}

clear
echo "-----------------------------------------"
echo "build_dockerfile_app_streaming $extension"
#build_dockerfile_app_streaming ${extension}
docker push logimethods/smart-meter:app-streaming${extension}
docker -H localhost:2374 pull logimethods/smart-meter:app-streaming${extension}

clear
echo "-----------------------------------------"
echo "build_dockerfile_app_batch $extension"
#build_dockerfile_app_batch ${extension}
docker push logimethods/smart-meter:app-batch${extension}
docker -H localhost:2374 pull logimethods/smart-meter:app-batch${extension}

clear
echo "-----------------------------------------"
echo "build_dockerfile_prometheus $extension"
#build_dockerfile_prometheus ${extension}
docker push logimethods/smart-meter:prometheus${extension}
docker -H localhost:2374 pull logimethods/smart-meter:prometheus${extension}

clear
echo "-----------------------------------------"
echo "build_dockerfile_monitor $extension"
#build_dockerfile_monitor ${extension}
docker push logimethods/smart-meter:monitor${extension}
docker -H localhost:2374 pull logimethods/smart-meter:monitor${extension}

clear
echo "-----------------------------------------"
echo "build_dockerfile_cassandra $extension"
#build_dockerfile_cassandra ${extension}
docker push logimethods/smart-meter:cassandra${extension}
docker -H localhost:2374 pull logimethods/smart-meter:cassandra${extension}

clear
echo "-----------------------------------------"
echo "build_dockerfile_telegraf $extension"
#build_dockerfile_telegraf ${extension}
docker push logimethods/smart-meter:telegraf${extension}
docker -H localhost:2374 pull logimethods/smart-meter:telegraf${extension}

clear
echo "-----------------------------------------"
echo "build_dockerfile_cassandra_inject $extension"
#build_dockerfile_cassandra_inject ${extension}
docker push logimethods/smart-meter:cassandra-inject${extension}
docker -H localhost:2374 pull logimethods/smart-meter:cassandra-inject${extension}

#docker push logimethods/prometheus-nats-exporter
#docker push logimethods/service-registry
