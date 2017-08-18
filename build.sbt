name := "samplecode.jung-clustering"

organization := "at.hazm"

version := "1.0"

scalaVersion := "2.12.3"

resolvers ++= Seq(
  "name" at "https://mvnrepository.com/artifact/net.sf.jung/jung-algorithms"
)

libraryDependencies ++= Seq(
  "net.sf.jung" % "jung-algorithms" % "2.1.1",
  "net.sf.jung" % "jung-graph-impl" % "2.1.1",
  "org.slf4j" % "slf4j-log4j12" % "1.7.+"
)
