package ch.insign.sbt.sjs_play_routes.parser

sealed trait RouteFileEntry

case class RouteEntry(httpMethod: HttpMethod, url: String, call: Call) extends RouteFileEntry

case class RouteFileReference(mountPoint: String, routesFile: String) extends RouteFileEntry
