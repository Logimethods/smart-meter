//@see https://github.com/marcuslonnberg/sbt-docker
//@see https://github.com/marcuslonnberg/sbt-docker/blob/master/examples/package-spray/build.sbt
//@see https://velvia.github.io/Docker-Scala-Sbt/

import sbt.Keys.{artifactPath, libraryDependencies, mainClass, managedClasspath, name, organization, packageBin, resolvers, version}
import com.typesafe.config.{ConfigFactory, Config}
import java.util.Properties

val appProperties = settingKey[Properties]("The application properties")
appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("../configuration.properties"))
  prop
}

// logLevel := Level.WARN

val rootName = "smart-meter"
name := "docker-" + rootName + "-app-batch"
organization := "logimethods"
val tag = "app-batch-local"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "Sonatype OSS Release" at "https://oss.sonatype.org/content/groups/public/"

version := "0.4.0-SNAPSHOT"
scalaVersion := "2.11.8"

lazy val sparkVersion = settingKey[String]("sparkVersion")
sparkVersion := {
  try {
    appProperties.value.getProperty("spark_version")
  } catch {
    case _: Exception => "<empty>"
  }
}

lazy val sparkCassandraConnectorVersion = settingKey[String]("sparkCassandraConnectorVersion")
sparkCassandraConnectorVersion := {
  try {
    appProperties.value.getProperty("spark_cassandra_connector_version")
  } catch {
    case _: Exception => "<empty>"
  }
}

lazy val natsConnectorSparkVersion = settingKey[String]("natsConnectorSparkVersion")
natsConnectorSparkVersion := {
  try {
    appProperties.value.getProperty("nats_connector_spark_version")
  } catch {
    case _: Exception => "<empty>"
  }
}

libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion.value % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion.value % "provided"
libraryDependencies += "com.datastax.spark" %% "spark-cassandra-connector" % sparkCassandraConnectorVersion.value

assemblyJarName in assembly := "docker-smart-meter-app-batch-assembly.jar"
