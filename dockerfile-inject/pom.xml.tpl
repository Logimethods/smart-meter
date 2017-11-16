<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

  <modelVersion>4.0.0</modelVersion>

  <groupId>org.deetazilla.app</groupId>
  <artifactId>app_inject</artifactId>
  <packaging>jar</packaging>
  <version>latest</version>
  <name>app_inject</name>

  <dependencies>
    <!-- https://mvnrepository.com/artifact/com.typesafe.scala-logging/scala-logging -->
    <dependency>
        <groupId>com.typesafe.scala-logging</groupId>
        <artifactId>scala-logging_2.11</artifactId>
        <version>${gatling_scala_logging_version}</version>
    </dependency>

    <dependency>
      <groupId>com.logimethods</groupId>
      <artifactId>nats-connector-gatling_2.11</artifactId>
      <version>${nats_connector_gatling_version}</version>
    </dependency>
  </dependencies>

  <repositories>
      <repository>
          <id>browserid-snapshots</id>
          <name>browserid-snapshots</name>
          <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
      </repository>
  </repositories>
</project>