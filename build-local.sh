pushd dockerfile-inject
sbt update
sbt docker
sbt eclipse
popd

pushd dockerfile-app-streaming
sbt update
sbt docker
sbt eclipse
popd

pushd dockerfile-monitor
sbt update
sbt docker
sbt eclipse
popd

pushd dockerfile-cassandra
docker build -t logimethods/smart-meter:cassandra-local .
popd

pushd dockerfile-cassandra-inject
docker build -t logimethods/smart-meter:cassandra-inject-local .
popd

pushd dockerfile-nats-server
docker build -t logimethods/smart-meter:nats-server-local .
popd
