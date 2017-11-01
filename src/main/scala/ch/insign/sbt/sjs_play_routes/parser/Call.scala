package ch.insign.sbt.sjs_play_routes.parser

case class Call(pkg: Option[String], className: String, method: String, params: List[Parameter] = List())
