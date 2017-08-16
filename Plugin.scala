package com.lightbend.tools.fortify.sbtplugin

import sbt._
import Keys._

object FortifyPlugin extends AutoPlugin {

  object autoImport {
    val translateCommand = Command.command("translate") { (state: State) =>
      Project.runTask(clean in Compile, state)
      Project.runTask(compile in Compile, state)
      state
    }
    val scanCommand = Command.command("scan") { (state: State) =>
      val fpr = "scan.fpr"
      Seq("bash","-c", s"rm -rf ${fpr}").!
      Seq("bash","-c", s"sourceanalyzer -filter filter.txt -f ${fpr} -scan target/*.nst").!
      state
    }
  }

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  import autoImport._

  override lazy val projectSettings = Seq(
    commands ++= Seq(translateCommand, scanCommand),
    credentials += Credentials(Path.userHome / ".lightbend" / "commercial.credentials"),
    resolvers += Resolver.url(
      "lightbend-commercial-releases",
      new URL("http://repo.lightbend.com/commercial-releases/"))(
      Resolver.ivyStylePatterns),
    autoCompilerPlugins := true,
    addCompilerPlugin(
      "com.lightbend" %% "scala-fortify" % "e940f40a" classifier "assembly"
        exclude("com.typesafe.conductr", "ent-suite-licenses-parser")
        exclude("default", "scala-st-nodes")),
    scalacOptions += s"-P:fortify:out=${target.value}"
  )

}
