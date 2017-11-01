package ch.insign.sbt.sjs_play_routes

import ch.insign.sbt.sjs_play_routes.parser.Parameter

final case class RouteMethod(method: String, name: String, pathSegment: String, params: Seq[Parameter])
