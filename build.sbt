ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "scala"
  )

val AkkaVersion = "2.8.0"
val AkkaHttpVersion = "10.5.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion,

  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-caching" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion,

  "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.39.2",
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
)