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

// logLevel := Level.Debug

val rootName = "smart-meter"
name := "docker-" + rootName + "-app-streaming"
organization := "logimethods"
val tag = "app-streaming-local"

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

lazy val natsConnectorSparkVersion = settingKey[String]("natsConnectorSparkVersion")
natsConnectorSparkVersion := {
  try {
    appProperties.value.getProperty("nats_connector_spark_version")
  } catch {
    case _: Exception => "<empty>"
  }
}

libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion.value % "provided"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % sparkVersion.value
libraryDependencies += "com.logimethods"  %% "nats-connector-spark-scala" % natsConnectorSparkVersion.value changing()

// @see http://stackoverflow.com/questions/30446984/spark-sbt-assembly-deduplicate-different-file-contents-found-in-the-followi
assemblyMergeStrategy in assembly := {
    case PathList("com", "esotericsoftware", minlog @ _*) => MergeStrategy.last
    case PathList("com", "google", common @ _*) => MergeStrategy.last
    case PathList("org", "apache", commons @ _*) => MergeStrategy.last
    case PathList("org", "apache", hadoop @ _*) => MergeStrategy.last
    case PathList("org", "slf4j", impl @ _*) => MergeStrategy.last
    case PathList("org", "glassfish", impl @ _*) => MergeStrategy.last
    case PathList("org.glassfish.hk2.external", "aopalliance-repackaged", impl @ _*) => MergeStrategy.last
    case PathList("org", "scalatest", impl @ _*) => MergeStrategy.last
    case PathList("org", "scalactic", impl @ _*) => MergeStrategy.last
    case PathList("org", "aopalliance", impl @ _*) => MergeStrategy.last
    case PathList("javax", "inject", impl @ _*) => MergeStrategy.last
    case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
}

enablePlugins(DockerPlugin)

imageNames in docker := Seq(
  ImageName(s"${organization.value}/${rootName}:${tag}")
)

// Define a Dockerfile
dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  //  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName)
    .mkString(":") + ":" + jarTarget

  new Dockerfile {
    // Use a base image that contain Scala
//    from("williamyeh/scala:2.10.4")
    from("frolvlad/alpine-scala:2.11")

    // Set the log4j.properties
    run("mkdir", "-p", "/usr/local/spark/conf")
    env("SPARK_HOME", "/usr/local/spark")
    copyToStageDir(file("spark/conf/log4j.properties"), file("log4j.properties"))
    copy("log4j.properties", "/usr/local/spark/conf")

    // Add all files on the classpath
    copy(classpath.files, "/app/")
    // Add the JAR file
    copy(jarFile, jarTarget)

    volume("/spark/storage")

    // On launch run Scala with the classpath and the main class
    // @see https://mail-archives.apache.org/mod_mbox/spark-dev/201312.mbox/%3CCAPh_B=ass2NcrN41t7KTSoF1SFGce=N57YMVyukX4hPcO5YN2Q@mail.gmail.com%3E
    // @see http://apache-spark-user-list.1001560.n3.nabble.com/spark-1-6-Issue-td25893.html
    entryPoint("java", "-Xms128m", "-Xmx512m", "-cp", classpathString, "-Dlog4j.configuration=file:/usr/local/spark/conf/log4j.properties" /*, mainclass*/)
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
  //  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName)
    .mkString(":") + ":" + jarTarget

  val dockerFile = new Dockerfile {
    // Use a base image that contain Scala
//    from("williamyeh/scala:2.10.4")
    from("frolvlad/alpine-scala:2.11")

    // Set the log4j.properties
    run("mkdir", "-p", "/usr/local/spark/conf")
    env("SPARK_HOME", "/usr/local/spark")
    copyToStageDir(file("spark/conf/log4j.properties"), file("log4j.properties"))
    copy("log4j.properties", "/usr/local/spark/conf")

    // Add all files on the classpath
    copy(classpath.files, "/app/")
    // Add the JAR file
    copy(jarFile, jarTarget)
    
    volume("/spark/storage")

    // On launch run Scala with the classpath and the main class
    // @see https://mail-archives.apache.org/mod_mbox/spark-dev/201312.mbox/%3CCAPh_B=ass2NcrN41t7KTSoF1SFGce=N57YMVyukX4hPcO5YN2Q@mail.gmail.com%3E
    // @see http://apache-spark-user-list.1001560.n3.nabble.com/spark-1-6-Issue-td25893.html
    entryPoint("java", "-Xms128m", "-Xmx512m", "-cp", classpathString, "-Dlog4j.configuration=file:/usr/local/spark/conf/log4j.properties" /*, mainclass*/)
  }

  val stagedDockerfile =  sbtdocker.staging.DefaultDockerfileProcessor(dockerFile, dockerDir)
  IO.write(dockerDir / "Dockerfile",stagedDockerfile.instructionsString)
  stagedDockerfile.stageFiles.foreach {
    case (source, destination) =>
      source.stage(destination)
  }
}

dockerFileTask <<= dockerFileTask.dependsOn(compile in Compile, dockerfile in docker)
