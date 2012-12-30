name := "scala.frp"

version := "1.0"

scalaVersion := "2.10.0-RC5"

libraryDependencies += "org.scalatest" % "scalatest_2.10.0-RC5" % "1.8-B1" % "test"

scalacOptions in (Compile, doc) ++= Seq( "-implicits")