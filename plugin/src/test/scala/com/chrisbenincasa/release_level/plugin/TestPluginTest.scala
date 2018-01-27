package com.chrisbenincasa.release_level.plugin

import org.scalatest.FunSuite
import scala.io.Source
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.{Global, Settings}

class TestPluginTest extends FunSuite { suite =>

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
    compile(filename)
  }

  test("unsuppressed") {
    testFile("test.scala")
  }

}
