<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

  <modelVersion>4.0.0</modelVersion>

  <groupId>org.deetazilla.app</groupId>
  <artifactId>app_monitor</artifactId>
  <packaging>jar</packaging>
  <version>latest</version>
  <name>app_monitor</name>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.scala-lang</groupId>
      <artifactId>scala-library</artifactId>
      <version>${scala_version}</version>
      <scope>provided</scope>
    </dependency>

    <dependency>
      <groupId>com.github.tyagihas</groupId>
      <artifactId>scala_nats_2.11</artifactId>
      <version>${scala_nats_version}</version>
    </dependency>

    <dependency>
      <groupId>io.nats</groupId>
      <artifactId>java-nats-streaming</artifactId>
      <version>${java_nats_streaming_version}</version>
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
          <version>3.6.2</version>
          <configuration>
            <source>1.8</source>
            <target>1.8</target>
          </configuration>
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
