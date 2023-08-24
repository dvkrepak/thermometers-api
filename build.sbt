ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "scala"
  )

val AkkaVersion = "2.8.0"
val AkkaHttpVersion = "10.5.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.typesafe.play" %% "play-ws-standalone-json" % "2.1.10",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-caching" % AkkaHttpVersion,

  "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.39.2",
  "io.spray" %% "spray-json" % "1.3.6",
  "com.github.sbt" % "junit-interface" % "0.13.3" % "test",
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  "org.mockito" %% "mockito-scala" % "1.17.12",
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion,

)