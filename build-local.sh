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

