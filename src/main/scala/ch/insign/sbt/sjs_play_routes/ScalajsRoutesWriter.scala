package ch.insign.sbt.sjs_play_routes

import ch.insign.sbt.sjs_play_routes.ScalajsPlayRoutes.{ControllerContents, PackageContents}
import ch.insign.sbt.sjs_play_routes.parser.{DefaultValueParameter, FixedValueParameter, Parameter}
import sbt.{Logger, _}

/**
  * Writes the routes to a directory
  */
class ScalajsRoutesWriter(targetDir: File, prefix: String, log: Logger) {

  private lazy val prefixSafe = if(prefix.isEmpty) "" else if(prefix.endsWith(".")) prefix else s"$prefix."

  private [sjs_play_routes] def writeRoutes(routes: Map[String, PackageContents]): Seq[File] =
    routes
      .toSeq
      .flatMap(pkg => writePackage(pkg._1, pkg._2))

  private def writePackage(packageName: String, content: PackageContents): Seq[File] = {
    log.info(s"Writing package... $packageName")
    content
      .toSeq
      .flatMap(ctrl => writeController(packageName, ctrl._1, ctrl._2))
  }

  private def writeController(packageName: String, controllerName: String, content: ControllerContents): Seq[File] = {
    log.info(s"Writing controller... $packageName.$controllerName")
    val file = new File(controllerFile(packageName, controllerName))
    log.debug(s"Writing to file: ${file.getAbsolutePath}")
    val fileContent =
      s"""package $prefixSafe$packageName
         |
         |import fr.hmil.roshttp.HttpRequest
         |import fr.hmil.roshttp.response.SimpleHttpResponse
         |import fr.hmil.roshttp.Method._
         |import fr.hmil.roshttp.body.BodyPart
         |import java.util.regex.Pattern
         |import scala.concurrent.Future
         |import monix.execution.Scheduler.Implicits.global
         |
         |/**
         | * Automatically generated
         | * Routes for $controllerName
         | */
         |object $controllerName {
         |
         |  class Call(request: HttpRequest) {
         |
         |    def send(): Future[SimpleHttpResponse] =
         |      request.send()
         |
         |    def send(bodyPart: BodyPart): Future[SimpleHttpResponse] =
         |      request.send(bodyPart)
         |
         |  }
         |
         |  private def _addUrlParam(request: HttpRequest, param: (String, String)): HttpRequest =
         |    request.withQueryParameter(param._1, param._2)
         |
         |  private def _replaceAll(search: String, replace: String, haystack: String, withRegex: Boolean = false): String =
         |    haystack.split(Pattern.quote(search) + _regex(withRegex), -1).mkString(replace)
         |
         |  private def _regex(on: Boolean): String =
         |    if(on)
         |      "(\\\\<([^>])*\\\\>)?"
         |    else
         |      ""
         |
         |  private def _foldParam(acc: (String, Map[String, String]), param: (String, String)): (String, Map[String, String]) =
         |    if(acc._1.contains(s":$${param._1}"))
         |      (_replaceAll(s":$${param._1}", s"$${param._2}", acc._1), acc._2)
         |    else if(acc._1.contains(s"*$${param._1}"))
         |      (_replaceAll(s"*$${param._1}", s"$${param._2}", acc._1), acc._2)
         |    else if(acc._1.contains(s"$$$$$${param._1}"))
         |      (_replaceAll(s"$$$$$${param._1}", s"$${param._2}", acc._1, true), acc._2)
         |    else
         |      (acc._1, acc._2 + param)
         |
         |${routeMethods(content)}
         |
         |}
       """.stripMargin
    // println(fileContent)
    IO.write(file, fileContent)
    Seq(file)
  }

  private def routeMethods(content: ControllerContents): String =
    content
      .flatMap(route => routeMethod(route._1, route._2))
      .mkString("\n\n")

  private def routeMethod(methodName: String, overloads: Seq[RouteMethod]): Seq[String] =
    Seq(
      s"""  def $methodName(${methodParams(overloads)})(implicit httpRequest: HttpRequest): Call =
         |    (${paramsForMatch(overloads)}) match {
         |${casesForMatch(overloads)}
         |    }
       """.stripMargin)

  private def methodParams(overloads: Seq[RouteMethod]): String =
    if(overloads.isEmpty)
      ""
    else
      overloads
        .head
        .params
        .map { param =>
          s"`${param.name}`: ${param.tpe}${paramDefault(param)}"
        }
        .mkString(", ")

  private def paramsForMatch(overloads: Seq[RouteMethod]): String =
    if(overloads.isEmpty)
      ""
    else
      overloads
        .head
        .params
        .map(param => s"`${param.name}`")
        .mkString(", ")

  private def casesForMatch(overloads: Seq[RouteMethod]): String =
    overloads.map { overload =>
      s"""      case (${paramsForCase(overload)}) ${ifForCase(overload)} =>
         |${routeMethod(overload)}
         |""".stripMargin
    }.mkString("\n")

  private def paramsForCase(overload: RouteMethod): String =
    overload
      .params
      .map(toParamForCase)
      .mkString(", ")

  private def toParamForCase(param: Parameter): String =
    s"`${param.name}`"

  private def ifForCase(overload: RouteMethod): String = {
    val ifConditions = overload.params.flatMap(toIfCondition)
    if(ifConditions.isEmpty)
      ""
    else
      ifConditions.mkString("if ", " && ", "")
  }

  private def toIfCondition(param: Parameter): Option[String] =
    param match {
      case DefaultValueParameter(_, _, _) => None
      case FixedValueParameter(_, _, value) => Some(s"${param.name} == $value")
      case _ => None
    }

  private def routeMethod(overload: RouteMethod): String =
    s"""        val params = ${routeParams(overload)}
       |        val urlAndParams: (String, Map[String, String]) =
       |          params
       |            .toSeq
       |            .sortBy(_._1.length)
       |            .reverse
       |            .foldLeft(
       |              ("${basicUrl(overload)}", Map[String, String]())
       |            )(_foldParam)
       |
       |${toRequest(overload.method)} """.stripMargin

  private def toRequest(method: String): String =
    s"""        new Call(
       |          urlAndParams._2
       |            .foldLeft(
       |              httpRequest
       |                .withMethod(${method.toUpperCase})
       |                .withPath(urlAndParams._1)
       |            )(_addUrlParam)
       |        )
     """.stripMargin

  private def basicUrl(overload: RouteMethod): String =
    overload
      .pathSegment
      .replace("\\", "\\\\")

  private def routeParams(overload: RouteMethod): String =
    s"""Map[String, String](
       |${replaceParams(overload)}
       |        )""".stripMargin

  private def replaceParams(overload: RouteMethod): String =
    overload
      .params
      .filterNot(_.isFixed)
      .map(toReplaceParam)
      .mkString(",\n")

  private def toReplaceParam(param: Parameter): String =
    s"""          "${param.name}" -> `${param.name}`.toString"""

  private def paramDefault(param: Parameter): String =
    param match {
      case DefaultValueParameter(_, _, value) => s" = $value"
      case _ => ""
    }

  private def controllerFile(packageName: String, controllerName: String): String =
    targetDir / s"$prefixSafe$packageName.$controllerName".replaceAll("\\.", "/") + ".scala"

}

