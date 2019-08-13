name := "multi-module"

version := "0.1"

scalaVersion := "2.13.0"
lazy val root = (project in file(".")).aggregate(util)

lazy val util = project in file("util")
lazy val core = (project in file("core")).dependsOn(util)