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

    <dependency>
      <groupId>org.scalanlp</groupId>
      <artifactId>breeze_2.11</artifactId>
      <version>${GATLING_breeze_version}</version>
    </dependency>

    <dependency>
      <groupId>org.scalactic</groupId>
      <artifactId>scalactic_2.11</artifactId>
      <version>${GATLING_scalactic_version}</version>
    </dependency>
    <dependency>
      <groupId>org.scalatest</groupId>
      <artifactId>scalatest_2.11</artifactId>
      <version>${GATLING_scalactic_version}</version>
      <scope>test</scope>
    </dependency>

    <dependency>
      <groupId>org.scala-lang</groupId>
      <artifactId>scala-library</artifactId>
      <version>${gatling_scala_version}</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>

  <repositories>
      <repository>
          <id>browserid-snapshots</id>
          <name>browserid-snapshots</name>
          <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
      </repository>
  </repositories>

  <!-- http://davidb.github.io/scala-maven-plugin/example_java.html -->
  <build>
    <sourceDirectory>src/main/scala</sourceDirectory>

    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>net.alchim31.maven</groupId>
          <artifactId>scala-maven-plugin</artifactId>
          <version>3.3.1</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>2.0.2</version>
        </plugin>
      </plugins>
    </pluginManagement>
    <plugins>
      <plugin>
        <groupId>net.alchim31.maven</groupId>
        <artifactId>scala-maven-plugin</artifactId>
        <executions>
          <execution>
            <id>scala-compile-first</id>
            <phase>process-resources</phase>
            <goals>
              <goal>add-source</goal>
              <goal>compile</goal>
            </goals>
          </execution>
          <execution>
            <id>scala-test-compile</id>
            <phase>process-test-resources</phase>
            <goals>
              <goal>testCompile</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
            <descriptorRefs>
                <descriptorRef>jar-with-dependencies</descriptorRef>
            </descriptorRefs>
        </configuration>
        <executions>
          <execution>
            <phase>compile</phase>
            <goals>
              <goal>compile</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-shade-plugin</artifactId>
          <version>3.1.0</version>
          <executions>
              <execution>
                  <phase>package</phase>
                  <goals>
                      <goal>shade</goal>
                  </goals>
                  <configuration>
                      <minimizeJar>true</minimizeJar>
                  </configuration>
              </execution>
          </executions>
      </plugin>
    </plugins>
  </build>
</project>