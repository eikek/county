import sbt._
import Keys._
import Dependencies._

object Resolvers {
  val eknet = "eknet.org" at "https://eknet.org/maven2"
}

object Version {
  val blueprints = "2.3.0"
  val grizzled = "0.6.10"
  val logback = "1.0.10"
  val orientdb = "1.3.0"
  val scala = "2.9.2"
  val scalaTest = "1.9.1"
  val slf4j = "1.7.4"
}

object Dependencies {
  val blueprintsCore = "com.tinkerpop.blueprints" % "blueprints-core" % Version.blueprints % "provided" intransitive()
  val blueprintsOrient = "com.tinkerpop.blueprints" % "blueprints-orient-graph" % Version.blueprints % "test"
  val grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % Version.grizzled exclude("org.slf4j", "slf4j-api") //scala 2.9.2 only
  val logbackClassic = "ch.qos.logback" % "logback-classic" % Version.logback  exclude("org.slf4j", "slf4j-api")
  val orientdb = "com.orientechnologies" % "orientdb-core" % Version.orientdb
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest % "test"
  val slf4jApi = "org.slf4j" % "slf4j-api" % Version.slf4j
}

object RootBuild extends Build {

  lazy val root = Project(
    id = "county",
    base = file("."),
    settings = buildSettings
  ) aggregate (
     Api.module,
     BlueprintsBackend.module
  )

  val buildSettings = Project.defaultSettings ++ Seq(
    name := "county"
  )

  override lazy val settings = super.settings ++ Seq(
    version := "0.1.0-SNAPSHOT",
    organization := "org.eknet.county",
    scalaVersion := Version.scala,
    publishTo := Some("eknet-maven2" at "https://eknet.org/maven2"),
    publishArtifact in Test := true,
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    crossScalaVersions := Seq("2.9.2", "2.9.3", "2.10.1"),
    exportJars := true,
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    resolvers := Seq(Resolvers.eknet),
    pomIncludeRepository := (_ => false),
    scmInfo := Some(ScmInfo(new URL("https://eknet.org/gitr/?r=county.git"), "scm:git:https://eknet.org/git/county.git")),
    licenses := Seq(("ASL2", new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")))
  )
}

object Api extends Build {
 
  lazy val module = Project(
    id = "county-api",
    base = file("api"),
    settings = buildSettings
  )

  lazy val buildSettings = Project.defaultSettings ++ Seq(
    name := "county-api",
    libraryDependencies ++= deps
  )

  lazy val deps = Seq(slf4jApi, scalaTest)
  
}

object BlueprintsBackend extends Build {

  lazy val module = Project(
    id = "county-backend-blueprints",
    base = file("backend-blueprints"),
    settings = buildSettings
  ) dependsOn(Api.module)

  lazy val buildSettings = Project.defaultSettings ++ Seq(
    name := "county-backend-blueprints",
    libraryDependencies ++= deps
  )

  lazy val deps = Seq(slf4jApi, blueprintsCore, blueprintsOrient, scalaTest)
}