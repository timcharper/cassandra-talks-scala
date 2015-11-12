name := "cassandra-talks-scala"

organization := "com.timcharper"

description := "Simple integration library which enriches a Cassandra session so you can easily return Scala Futures and Akka Streams"

scalaVersion := "2.11.7"

version := "0.1"

libraryDependencies := Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.8",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0-M1"
)

homepage := Some(url("https://github.com/timcharper/cassandra-talks-scala"))

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

pomExtra := {
  <scm>
    <url>https://github.com/timcharper/cassandra-talks-scala</url>
    <connection>scm:git:git@github.com:timcharper/cassandra-talks-scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>timcharper</id>
      <name>Tim Harper</name>
      <url>http://timcharper.com</url>
    </developer>
  </developers>
}

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false
