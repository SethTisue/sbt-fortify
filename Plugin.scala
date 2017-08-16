package com.lightbend.tools.fortify.sbtplugin

import sbt._
import Keys._

object FortifyPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  // thank you http://stackoverflow.com/a/37271468/86485 !

  object autoImport {
    val translateCommand = Command.command("translate") {state =>
      // Project.runTask(clean in Compile, state)
      val binaryVersion = {
        val extracted: Extracted = Project.extract(state)
        import extracted._
        val thisScope = Load.projectScope(currentRef)
        (scalaBinaryVersion in thisScope get extracted.structure.data).get
      }
      val compilerPluginJar = for {
        (newState, result) <- Project.runTask(fullClasspath in Compile, state)
        cp <- result.toEither.right.toOption
        jar <- cp.find(
          _.get(moduleID.key).exists{mId =>
            println(mId)
            mId.organization == "com.lightbend" &&
              mId.name == s"scala-fortify_$binaryVersion"})
      } yield jar.data.absolutePath
      println(compilerPluginJar.get)
      val targetDir = {
        val extracted: Extracted = Project.extract(state)
        import extracted._
        val thisScope = Load.projectScope(currentRef)
        (target in thisScope get extracted.structure.data).get
      }
      println(targetDir)
      Project.runTask(scalacOptions, state) match {
        case Some((newState, result)) =>
          result.toEither.right.foreach { defaultScalacOptions =>
            Project.runTask(compile in Compile,
              Project.extract(state).append(
                scalacOptions :=
                  defaultScalacOptions ++
                  compilerPluginJar.map(p => Seq(s"-Xplugin:$p")).getOrElse(Seq.empty) ++
                  Seq("-Ystop-before:jvm", "-Xplugin-require:fortify", s"-P:fortify:out=$targetDir"),
                newState))}
        case None =>
          sys.error("Couldn't get default scalacOptions")
      }
      state
    }

    val scanCommand = Command.command("scan") {state =>
      val fpr = "scan.fpr"
      IO.delete(new java.io.File(fpr))
      val targetDir = {
        val extracted: Extracted = Project.extract(state)
        import extracted._
        val thisScope = Load.projectScope(currentRef)
        (target in thisScope get extracted.structure.data).get
      }
      val nstFiles = (targetDir ** "*.nst").get.map(_.toString)
      (Seq("sourceanalyzer", "-filter", "filter.txt", "-f", fpr, "-scan") ++ nstFiles).!
      state
    }
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    commands ++= Seq(translateCommand, scanCommand),
    credentials += Credentials(Path.userHome / ".lightbend" / "commercial.credentials"),
    resolvers += Resolver.url(
      "lightbend-commercial-releases",
      new URL("http://repo.lightbend.com/commercial-releases/"))(
      Resolver.ivyStylePatterns),
    addCompilerPlugin(
      "com.lightbend" %% "scala-fortify" % "e940f40a" classifier "assembly"
        exclude("com.typesafe.conductr", "ent-suite-licenses-parser")
        exclude("default", "scala-st-nodes"))
  )

}
