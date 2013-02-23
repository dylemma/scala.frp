name := "scala.frp"

organization := "com.dylemma"

version := "1.0"

scalaVersion := "2.10.0"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

scalacOptions in (Compile, doc) ++= Seq("-implicits")