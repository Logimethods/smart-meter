//@see https://github.com/marcuslonnberg/sbt-docker
//@see https://github.com/marcuslonnberg/sbt-docker/blob/master/examples/package-spray/build.sbt
//@see https://velvia.github.io/Docker-Scala-Sbt/

import sbt.Keys.{artifactPath, libraryDependencies, mainClass, managedClasspath, name, organization, packageBin, resolvers, version}

logLevel := Level.Debug

name := "nats-connector-spark"
organization := "logimethods"
val tag = "inject"

version := "0.3.0-SNAPSHOT"
scalaVersion := "2.11.8"
val gatlingVersion = "2.2.2"
val natsConnectorGatlingVersion = "0.3.0-SNAPSHOT"

libraryDependencies ++= Seq("com.logimethods" %% "nats-connector-gatling" % natsConnectorGatlingVersion)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "Sonatype OSS Release" at "https://oss.sonatype.org/content/groups/public/"

enablePlugins(DockerPlugin)

imageNames in docker := Seq(
  ImageName(s"${organization.value}/${name.value}:${tag}")
)

// Define a Dockerfile
dockerfile in docker := {
  val classpath = (managedClasspath in Compile).value

  new Dockerfile {
    // Use a base image that contain Gatling
	from("denvazh/gatling:" + gatlingVersion)
    // Add all files on the classpath
    add(classpath.files, "./lib/")
    // Add Gatling User Files
    add(baseDirectory.value / "user-files", "./user-files")
    
//    cmd("--no-reports", "-s", "com.logimethods.nats.demo.NatsInjection")
  }
}

// sbt dockerFileTask
// See https://github.com/marcuslonnberg/sbt-docker/issues/34

val dockerFileTask = taskKey[Unit]("Prepare the dockerfile and needed files")

dockerFileTask := {
  val dockerDir = target.value / "docker"
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  val classpath = (managedClasspath in Compile).value

  val dockerFile = new Dockerfile {
    // Use a base image that contain Gatling
	from("denvazh/gatling:" + gatlingVersion)
    // Add all files on the classpath
    add(classpath.files, "./lib/")
    // Add Gatling User Files
    add(baseDirectory.value / "user-files", "./user-files")

//    cmd("--no-reports", "-s", "com.logimethods.nats.demo.NatsInjection")
  }

  val stagedDockerfile =  sbtdocker.staging.DefaultDockerfileProcessor(dockerFile, dockerDir)
  IO.write(dockerDir / "Dockerfile",stagedDockerfile.instructionsString)
  stagedDockerfile.stageFiles.foreach {
    case (source, destination) =>
      source.stage(destination)
  }
}

dockerFileTask <<= dockerFileTask.dependsOn(compile in Compile, dockerfile in docker)