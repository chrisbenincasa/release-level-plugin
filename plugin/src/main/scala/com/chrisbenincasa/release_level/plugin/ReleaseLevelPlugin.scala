package com.chrisbenincasa.release_level.plugin

import com.chrisbenincasa.release_level.model.{PreAlpha, ReleaseStage}
import scala.annotation.tailrec
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

    override val phaseName: String = "release-level-analysis"
    override val runsAfter: List[String] = List("typer")

    private val annoTpes = ReleaseStage.values.toList.sorted.map(stage => {
      stage -> rootMirror.staticClass(stage.getAnnoClazz.getName)
    })

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
            case Annotated(annot, arg) if !arg.isDef =>
              extractReleaseStage(annot).map(arg -> _).toList
            case typed @ Typed(_, tpt) if tpt.tpe != null =>
              tpt.tpe.annotations.flatMap(ai => extractReleaseStage(ai.tree)).map(typed -> _)
            case md if md.symbol != null && !md.isDef =>
              val sym = md.symbol match {
                case s if s.isAccessor => s.accessedOrSelf
                case _ => md.symbol
              }
              sym.annotations.flatMap(ai => extractReleaseStage(ai.tree)).map(md -> _)
            case _ => Nil
          }
        }

        def processTree(root: Tree) = {
          @tailrec
          def loop(t: List[Tree], accum: List[(Tree, ReleaseStage)] = Nil): List[(Tree, ReleaseStage)] = {
            t match {
              case EmptyTree :: Nil | Nil => accum
              case EmptyTree :: ts => loop(ts, accum)
              case t0 :: tn =>
                val tp = List(t0, analyzer.macroExpandee(t0)).flatMap(handleTree)
                // Dont queue children of a getter's definition. This is compiler generated code for
                // val getters and we dont want to emit warnings on them
                if (t0.symbol != null && t0.isDef && t0.symbol.isGetter) {
                  loop(tn, accum ::: tp)
                } else {
                  loop(t0.children ++ tn, accum ::: tp)
                }
            }
          }

          loop(root :: Nil)
        }

        processTree(unit.body).foreach {
          case (tree, stage) if stage == ReleaseStage.PREALPHA =>
            reporter.warning(tree.pos, "Using experimental code! Turn this on explicitly!")
          case (tree, stage) =>
            reporter.echo(tree.pos, s"found an annotation for stage ${stage}!")
        }
      }
    }
  }
}

