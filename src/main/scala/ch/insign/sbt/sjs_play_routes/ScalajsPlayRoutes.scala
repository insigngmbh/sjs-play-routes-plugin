package ch.insign.sbt.sjs_play_routes

import ch.insign.sbt.sjs_play_routes.ScalajsPlayRoutes.PackageContents
import ch.insign.sbt.sjs_play_routes.parser.{RouteEntry, RouteFileEntry, RouteFileReference, RoutesParser}
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.Def.Setting
import sbt.Keys.{libraryDependencies, sourceGenerators, sourceManaged, streams}
import sbt._

class ScalajsPlayRoutes(log: Logger, fileName: String, targetDir: File, prefix: String) {

  private def parseRoutesFile(): Seq[File] = {
    RoutesParser.parseFile(new File(fileName)) match {
      case Left(errors) => exitWithParserErrors(errors)
      case Right(entries) => {
        new ScalajsRoutesWriter(targetDir, prefix, log).writeRoutes(
          toPackageContents(resolveReferences(entries))
        )
      }
    }
  }

  // TODO
  private def resolveReferences(routesEntries: List[RouteFileEntry]): List[RouteEntry] =
    routesEntries.flatMap {
      case x: RouteEntry => Some(x)
      case _: RouteFileReference => {
        // throw new NotImplementedError("References to other routes files are not implemented yet.")
        log.warn("References to other routes files are not implemented yet.")
        None
      }
    }

  private def toPackageContents(routesEntries: List[RouteEntry]): Map[String, PackageContents] =
    groupByPackage(routesEntries)
      .mapValues(groupByControllerName)
      .mapValues(_.mapValues(groupByActionName))


  private def groupByPackage(routesEntries: List[RouteEntry]): Map[String, List[RouteEntry]] =
    routesEntries
      .groupBy(_.call.pkg.getOrElse(""))

  private def groupByControllerName(routesEntries: List[RouteEntry]): Map[String, List[RouteEntry]] =
    routesEntries
      .groupBy(_.call.className)

  private def groupByActionName(routesEntries: List[RouteEntry]): Map[String, List[RouteMethod]] =
    routesEntries
      .groupBy(_.call.method)
      .mapValues(_.map(toRouteMethod))

  private def toRouteMethod(e: RouteEntry): RouteMethod =
    RouteMethod(
      e.httpMethod.toString,
      e.call.method,
      e.url,
      e.call.params
    )

  private def exitWithParserErrors(errors: List[String]): Seq[File] = {
    log.error(s"Could not parse file: $fileName")
    errors.foreach(e =>
      log.error(e)
    )
    throw new IllegalStateException(s"The routes-file '$fileName' could not be parsed.") // no files were written
  }

}

/**
 * Generates the routes for the client side of the project
 */
object ScalajsPlayRoutes extends AutoPlugin {

  type ControllerContents = Map[String, List[RouteMethod]]

  type PackageContents = Map[String, ControllerContents]

  object autoImport {

    lazy val scalajsPlayRoutes: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Generates play routes for the scalajs project")

    lazy val scalajsPlayRoutesFile: SettingKey[String] =
      settingKey[String]("The path to the play routes file")

    lazy val scalajsPlayRoutesPrefix: SettingKey[String] =
      settingKey[String]("The prefix for play routes")

  }

  import ScalaJSPlugin.autoImport._
  import autoImport._

  override def requires: Plugins = ScalaJSPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    scalajsPlayRoutesPrefix := "routes",
    scalajsPlayRoutes := {
      new ScalajsPlayRoutes(streams.value.log, scalajsPlayRoutesFile.value, sourceManaged.value, scalajsPlayRoutesPrefix.value).parseRoutesFile()
    },
    sourceGenerators in Compile += scalajsPlayRoutes.taskValue,
    libraryDependencies += "fr.hmil" %%% "roshttp" % "2.0.1"
  )

}

