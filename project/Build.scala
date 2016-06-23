import sbt._
import Keys._

object BuildDef extends Build {

	val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6"

	lazy val scalaFRP = Project("scala-frp", file("."))
		//project settings
		.settings(
			organization := "io.dylemma",
			version := "1.2",
			scalaVersion := "2.11.8",
			libraryDependencies += scalaTest % "test",
			scalacOptions in (Compile, doc) += "-implicits"
		)
		//publishing settings
		.settings(
			publishMavenStyle := true,
			publishArtifact in Test := false,
			pomIncludeRepository := { _ => false },
			pomExtra := pomExtraXml,
			publishTo <<= version { (v: String) =>
				val nexus = "https://oss.sonatype.org/"
				if(v.trim.endsWith("SNAPSHOT"))
					Some("snapshots" at nexus + "content/repositories/snapshots")
				else
					Some("releases" at nexus + "service/local/staging/deploy/maven2")
			}
		)

	lazy val pomExtraXml = (
		<url>https://github.com/dylemma/scala.frp</url>
		<licenses>
			<license>
				<name>MIT License</name>
				<url>http://opensource.org/licenses/mit-license.php</url>
				<distribution>repo</distribution>
			</license>
		</licenses>
		<scm>
			<url>git@github.com:dylemma/scala.frp.git</url>
			<connection>scm:git:git@github.com:dylemma/scala.frp.git</connection>
		</scm>
		<developers>
			<developer>
				<id>dylemma</id>
				<name>Dylan Halperin</name>
				<url>http://dylemma.io/</url>
			</developer>
		</developers>
	)
}