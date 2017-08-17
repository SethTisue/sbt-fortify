package com.lightbend.tools.fortify.sbtplugin

import sbt._
import Keys._

object FortifyPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    commands ++= Seq(
      Command.command("translate")(translate),
      Command.command("scan")(scan)),
    credentials += Credentials(Path.userHome / ".lightbend" / "commercial.credentials"),
    resolvers += Resolver.url(
      "lightbend-commercial-releases",
      new URL("http://repo.lightbend.com/commercial-releases/"))(
      Resolver.ivyStylePatterns),
    // can we somehow keep this off the compilation classpath?
    libraryDependencies +=
      "com.lightbend" %% "scala-fortify" % "e940f40a" classifier "assembly"
        exclude("com.typesafe.conductr", "ent-suite-licenses-parser")
        exclude("default", "scala-st-nodes")
  )

  /// helpers

  def settingValue[T](state: State, key: SettingKey[T]): T = {
    val extracted: Extracted = Project.extract(state)
    import extracted._
    val thisScope = Load.projectScope(currentRef)
      (key in thisScope get extracted.structure.data).get
  }

  def compilerPluginJar(state: State): Option[String] = {
    val binaryVersion = settingValue(state, scalaBinaryVersion)
    for {
      (newState, result) <- Project.runTask(fullClasspath in Compile, state)
      cp <- result.toEither.right.toOption
      jar <- cp.find(
        _.get(moduleID.key).exists(mId =>
          mId.organization == "com.lightbend" &&
            mId.name == s"scala-fortify_$binaryVersion"))
    } yield jar.data.absolutePath
  }

  // thank you http://stackoverflow.com/a/37271468/86485 !
  def translate(state: State): State = {
    val targetDir = settingValue(state, target)
    val moreOptions = Seq[String](
      s"-Xplugin:${compilerPluginJar(state).get}",
      "-Ystop-before:jvm",
      "-Xplugin-require:fortify",
      s"-P:fortify:out=$targetDir")
    Project.runTask(clean in Compile, state)
    Project.runTask(scalacOptions, state) match {
      case Some((newState, result)) =>
        result.toEither.right.foreach {options =>
          Project.runTask(compile in Compile,
            Project.extract(state).append(
              scalacOptions := options ++ moreOptions,
              newState))}
      case None =>
        sys.error("Couldn't get default scalacOptions")
    }
    state
  }

  def scan(state: State): State = {
    val fpr = "scan.fpr"
    IO.delete(new java.io.File(fpr))
    val targetDir = settingValue(state, target)
    val nstFiles = (targetDir ** "*.nst").get.map(_.toString)
    (Seq("sourceanalyzer", "-filter", "filter.txt", "-f", fpr, "-scan") ++ nstFiles).!
      state
  }

}
