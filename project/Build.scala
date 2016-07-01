import sbt._
import Keys._
import bintray.BintrayPlugin.autoImport._

object BuildDef extends Build {

	val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6"

	lazy val scalaFRP = Project("scala-frp", file("."))
		//project settings
		.settings(
			organization := "com.github.thangiee",
			version := "1.2",
			scalaVersion := "2.11.8",
			libraryDependencies += scalaTest % "test",
			scalacOptions in (Compile, doc) += "-implicits",
			publishMavenStyle := true,
			resolvers += Resolver.jcenterRepo,
      licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
      bintrayVcsUrl := Some("https://github.com/Thangiee/scala.frp")
		)
}