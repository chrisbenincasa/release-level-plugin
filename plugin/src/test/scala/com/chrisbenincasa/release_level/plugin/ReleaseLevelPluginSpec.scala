package com.chrisbenincasa.release_level.plugin

import org.scalatest.FunSuite
import scala.io.Source
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.{Global, Settings}

class ReleaseLevelPluginSpec extends FunSuite { suite =>

  val testdata = "plugin/testfiles/"
  val settings = new Settings
  settings.deprecation.value = true

  Option(getClass.getResourceAsStream("/embeddedcp")) match {
    case Some(is) =>
      Source.fromInputStream(is).getLines().foreach(settings.classpath.append)
    case None =>
      settings.usejavacp.value = true
  }

  // avoid saving classfiles to disk
  val outDir = new VirtualDirectory("(memory)", None)
  settings.outputDirs.setSingleOutput(outDir)
  val reporter = new ConsoleReporter(settings)

  val global = new Global(settings, reporter) { g =>
    override protected def loadRoughPluginsList() =
      new ReleaseLevelPlugin(this) :: super.loadRoughPluginsList()
  }

  def compile(filenames: String*): Unit = {
    val run = new global.Run
    run.compile(filenames.toList.map(testdata + _))
  }

  def testFile(filename: String): Unit = {
    compile(s"${filename}.scala")
  }

  test("unsuppressed") {
    testFile("test")
  }

  test("with options") {
    withPluginOptionsValue("test-scalac-compiler-plugin:>=alpha,<gamma" :: Nil) {
      testFile("test")
    }
  }

  test("vals") {
    testFile("vals")
  }

  test("deep_nesting") {
    testFile("deep_nesting")
  }


  private def withPluginOptionsValue(v: List[String])(f: => Unit): Unit = {
    val prev = settings.pluginOptions.value
    settings.pluginOptions.value = v
    f
    settings.pluginOptions.value = prev
  }
}
