name := "sbt-fortify"
sbtPlugin := true
scalaVersion := "2.10.6"
scalacOptions ++= Seq(
  "-deprecation", "-feature", "-unchecked", "-Xfatal-warnings")

organization := "com.lightbend"
licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))
homepage := scmInfo.value map (_.browseUrl)
scmInfo := Some(ScmInfo(
  url("https://github.com/lightbend/sbt-fortify"),
  "scm:git:git@github.com:lightbend/sbt-fortify.git"))
startYear := Some(2017)
description := "An sbt plugin to invoke HPE Fortify SCA"

publishMavenStyle := false
publishArtifact in Test := false
bintrayOrganization := None
bintrayRepository := "sbt-plugins"
bintrayReleaseOnPublish := false
