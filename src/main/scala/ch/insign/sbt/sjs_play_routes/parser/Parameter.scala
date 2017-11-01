package ch.insign.sbt.sjs_play_routes.parser

sealed trait Parameter {
  def name: String
  def tpe: String
  def isDefault: Boolean
  def isFixed: Boolean
}

case class NormalParameter(name: String, tpe: String) extends Parameter {
  override def isDefault = false
  override def isFixed = false

}

final case class DefaultValueParameter(name: String, tpe: String, defaultValue: String) extends Parameter {
  override def isDefault = true
  override def isFixed = false
}

final case class FixedValueParameter(name: String, tpe: String, value: String) extends Parameter {
  override def isDefault = false
  override def isFixed = true
}
