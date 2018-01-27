import BuildConfig.versions
import sbt._

lazy val commonSettings = BuildConfig.commonSettings(currentVersion = "1.0")

lazy val showVersion = taskKey[Unit]("Show version")

showVersion := {
  println(version.value)
}

lazy val model = (project in file("model")).
  settings(commonSettings).
  settings(
    name := "release-level-model"
  )

val saveTestClasspath = taskKey[File](
  "Saves test classpath to a file so that it can be used by embedded scalac in tests")

lazy val plugin = (project in file("plugin")).
  settings(commonSettings).
  settings(
    name := "release-level-plugin",
    libraryDependencies := Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalatest" %% "scalatest" % versions.scalatest % "test"
    ),
    saveTestClasspath := {
      val result = (classDirectory in Test).value / "embeddedcp"
      IO.write(result, (fullClasspath in Test).value.map(_.data.getAbsolutePath).mkString("\n"))
      result
    },
    (test in Test) := (test in Test).dependsOn(saveTestClasspath).value,
    fork in Test := true,
    baseDirectory in Test := (baseDirectory in ThisBuild).value
  ).dependsOn(model)

lazy val root = (project in file(".")).
  settings(commonSettings).
  settings(
    name := "beta plugin"
  ).aggregate(plugin, model)

// custom alias to hook in any other custom commands
addCommandAlias("build", "; compile")
