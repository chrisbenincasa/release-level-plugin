package com.chrisbenincasa.release_level.plugin

import com.chrisbenincasa.release_level.model.ReleaseStage
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.{Global, Phase}

class ReleaseLevelPlugin(override val global: Global) extends Plugin { self =>
  override val name: String = "test-scalac-compiler-plugin"
  override val description: String = "does a thing"
  override val components: List[PluginComponent] = List(ReleaseLevelPluginComponent)

  private val validOptions =
    for { 
      bound <- Set("<", "<=", ">", ">=", "=", "!=")
      stage <- ReleaseStage.values().map(_.toString.toLowerCase())
    } yield s"$bound$stage"

  override def init(options: List[String], error: String => Unit): Boolean = {
    val opts = options.flatMap(_.split(","))
    println(opts.mkString(" "))
    println(opts.map(_.toLowerCase()).toSet intersect validOptions)
    true
  }

  private object ReleaseLevelPluginComponent extends PluginComponent {
    override val global: Global = self.global

    import global._

    override val phaseName: String = "test-compiler-phase"
    override val runsAfter: List[String] = List("typer")

    private val annoTpes = ReleaseStage.values.toList.sorted.map(stage => stage -> rootMirror.staticClass(stage.getAnnoClazz.getName))

    reporter.echo(annoTpes.mkString(","))

    override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        reporter.echo(s"passed options = ${options}")

        def extractReleaseStage(tree: Tree): Option[ReleaseStage] = {
          if (tree.tpe == null) {
            None
          } else {
            annoTpes.find {
              case (_, clazz) =>
                val ref = TypeRef(NoType, clazz, Nil)
                tree.tpe <:< ref
            }.map(_._1)
          }
        }

        def handleTree(tree: Tree) = {
          tree match {
            case Annotated(annot, arg) =>
              extractReleaseStage(annot).map(arg -> _).toList
            case typed @ Typed(_, tpt) if tpt.tpe != null =>
              tpt.tpe.annotations.flatMap(ai => extractReleaseStage(ai.tree)).map(typed -> _)
            case md: MemberDef =>
              md.symbol.annotations.flatMap(ai => extractReleaseStage(ai.tree)).map(md -> _)
            case _ => Nil
          }
        }

        def allTrees(tree: Tree): Stream[Tree] =
          Stream(tree, analyzer.macroExpandee(tree)).filter(_ != EmptyTree).
            flatMap(t => t #:: t.children.toStream.flatMap(allTrees))

        allTrees(unit.body).flatMap(handleTree).toList.foreach {
          case (tree, stage) =>
            reporter.echo(tree.pos, s"found an annotation for stage ${stage}!")
        }
      }
    }
  }
}

