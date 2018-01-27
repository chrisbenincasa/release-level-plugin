package com.chrisbenincasa.release_level.plugin

import com.chrisbenincasa.release_level.model.ReleaseStage
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.{Global, Phase}

class ReleaseLevelPlugin(override val global: Global) extends Plugin { self =>
  override val name: String = "test-scalac-compiler-plugin"
  override val description: String = "does a thing"
  override val components: List[PluginComponent] = List(TestPluginComponent)

  private val validOptions =
    (for { x <- Set("<", "<=", ">", ">=", "=", "!="); y <- Set("alpha", "beta", "gamma") } yield (x, y)).
      map { case (x, y) => x + "" + y }

  override def init(options: List[String], error: String => Unit): Boolean = {
    println(options.mkString(" "))
    println(options.toSet intersect validOptions)
    true
  }

  private object TestPluginComponent extends PluginComponent {
    override val global: Global = self.global

    import global._

    override val phaseName: String = "test-compiler-phase"
    override val runsAfter: List[String] = List("typer")

    private val annoTpes = ReleaseStage.values.toList.sorted.map(stage => stage -> rootMirror.staticClass(stage.getAnnoClazz.getName))

    reporter.echo(annoTpes.mkString(","))

    override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        reporter.echo(s"passed options = ${options}")

        def releaseLevel(tree: Tree): Option[ReleaseStage] = {
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

        def betaTree(tree: Tree) = tree match {
          case Annotated(annot, arg) =>
            releaseLevel(annot).map(arg -> _).toList
          case typed @ Typed(_, tpt) if tpt.tpe != null && tpt.tpe.annotations.exists(ai => releaseLevel(ai.tree).isDefined) =>
            tpt.tpe.annotations.flatMap(ai => releaseLevel(ai.tree)).map(typed -> _)
          case md: MemberDef if md.symbol.annotations.exists(ai => releaseLevel(ai.tree).isDefined) =>
            md.symbol.annotations.flatMap(ai => releaseLevel(ai.tree)).map(md -> _)
          case _ => Nil
        }

        def allTrees(tree: Tree): Iterator[Tree] =
          Iterator(tree, analyzer.macroExpandee(tree)).filter(_ != EmptyTree).
            flatMap(t => Iterator(t) ++ t.children.iterator.flatMap(allTrees))

        allTrees(unit.body).flatMap(betaTree).toList.foreach {
          case (tree, stage) =>
            reporter.echo(tree.pos, s"found an annotation for stage ${stage}!")
        }
      }
    }
  }
}

