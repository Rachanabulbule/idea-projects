name := "shapeless"

version := "0.1"

scalaVersion := "2.11.7"
val akkaVersion = "2.5.1"

libraryDependencies ++= Seq("com.chuusai" %% "shapeless" % "2.3.0")
/*libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)*/