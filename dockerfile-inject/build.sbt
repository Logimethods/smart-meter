//@see https://github.com/marcuslonnberg/sbt-docker
//@see https://github.com/marcuslonnberg/sbt-docker/blob/master/examples/package-spray/build.sbt
//@see https://velvia.github.io/Docker-Scala-Sbt/

import sbt.Keys.{artifactPath, libraryDependencies, mainClass, managedClasspath, name, organization, packageBin, resolvers, version}
import com.typesafe.config.{ConfigFactory, Config}
import java.util.Properties

val appProperties = settingKey[Properties]("The application properties")
appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("../properties/configuration.properties"))
  prop
}

logLevel := Level.Debug

val rootName = "smart-meter"
name := "docker-" + rootName + "-inject"
organization := "logimethods"
val tag = "inject-local"

version := "0.4.0-SNAPSHOT"
scalaVersion := "2.11.8"

lazy val gatlingVersion = settingKey[String]("gatlingVersion")
gatlingVersion := {
  try {
    appProperties.value.getProperty("gatling_version")
  } catch {
    case _: Exception => "<empty>"
  }
}

lazy val natsConnectorGatlingVersion = settingKey[String]("natsConnectorGatlingVersion")
natsConnectorGatlingVersion := {
  try {
    appProperties.value.getProperty("nats_connector_gatling_version")
  } catch {
    case _: Exception => "<empty>"
  }
}

libraryDependencies += "com.logimethods" %% "nats-connector-gatling" % natsConnectorGatlingVersion.value changing()
// https://mvnrepository.com/artifact/org.scalanlp/breeze_2.11
libraryDependencies += "org.scalanlp" % "breeze_2.11" % "0.12"
libraryDependencies += "io.gatling" % "gatling-core" % gatlingVersion.value

// http://www.scalatest.org/install
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "Sonatype OSS Release" at "https://oss.sonatype.org/content/groups/public/"

enablePlugins(DockerPlugin)

imageNames in docker := Seq(
  ImageName(s"${organization.value}/${rootName}:${tag}")
)

// Define a Dockerfile
dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val jarTarget = s"./lib/${jarFile.getName}"

  new Dockerfile {
    // Use a base image that contain Gatling
	from("denvazh/gatling:" + gatlingVersion.value)
    // Add all files on the classpath
    add(classpath.files, "./lib/")
    // Add the JAR file
    copy(jarFile, jarTarget)
    // Add Gatling User Files
    add(baseDirectory.value / "user-files", "./user-files")
    // Add Gatling Configuration Files
    add(baseDirectory.value / "conf", "./conf")

//    cmd("--no-reports", "-s", "com.logimethods.smartmeter.inject.NatsInjection")
  }
}

// sbt dockerFileTask
// See https://github.com/marcuslonnberg/sbt-docker/issues/34

val dockerFileTask = taskKey[Unit]("Prepare the dockerfile and needed files")

dockerFileTask := {
  val dockerDir = target.value / "docker"
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val jarTarget = s"./lib/${jarFile.getName}"

  val dockerFile = new Dockerfile {
    // Use a base image that contain Gatling
	from("denvazh/gatling:" + gatlingVersion.value)
    // Add all files on the classpath
    add(classpath.files, "./lib/")
    // Add the JAR file
    copy(jarFile, jarTarget)
    // Add Gatling User Files
    add(baseDirectory.value / "user-files", "./user-files")
    // Add Gatling Configuration Files
    add(baseDirectory.value / "conf", "./conf")

//    cmd("--no-reports", "-s", "com.logimethods.smartmeter.inject.NatsInjection")
  }

  val stagedDockerfile =  sbtdocker.staging.DefaultDockerfileProcessor(dockerFile, dockerDir)
  IO.write(dockerDir / "Dockerfile",stagedDockerfile.instructionsString)
  stagedDockerfile.stageFiles.foreach {
    case (source, destination) =>
      source.stage(destination)
  }
}

dockerFileTask <<= dockerFileTask.dependsOn(compile in Compile, dockerfile in docker)
