package ch.insign.sbt.sjs_play_routes.parser

import atto._
import Atto._
import atto.compat.stdlib._
import sbt.File

object RoutesParser {

  private val dot: Parser[Char] = char('.') namedOpaque "dot"

  private val comma: Parser[Char] = char(',') namedOpaque "comma"

  private val colon: Parser[Char] = char(':') namedOpaque "colon"

  private val semicolon: Parser[Char] = char(';') namedOpaque "semicolon"

  private val qmark: Parser[Char] = char('?') namedOpaque "qmark"

  private val equal: Parser[Char] = char('=') namedOpaque "equal"

  private val quote: Parser[Char] = char('"') namedOpaque "quote"

  private[parser] val parseHttpMethod: Parser[HttpMethod] = {
    stringCI("GET")     .map(_ => GET     : HttpMethod) |
    stringCI("PUT")     .map(_ => PUT     : HttpMethod) |
    stringCI("POST")    .map(_ => POST    : HttpMethod) |
    stringCI("DELETE")  .map(_ => DELETE  : HttpMethod) |
    stringCI("OPTIONS") .map(_ => OPTIONS : HttpMethod) |
    stringCI("PATCH")   .map(_ => PATCH   : HttpMethod) |
    err[HttpMethod]("Unable to parse the HTTP method")
  }

  private[parser] val parseUrlSegment: Parser[String] =
    takeWhile(!_.isWhitespace) namedOpaque "url segment"

  private[parser] val parseJavaIdentifierStart: Parser[Char] =
    satisfy(Character.isJavaIdentifierStart) namedOpaque "java identifier start"

  private[parser] val parseJavaIdentifierPart: Parser[Char] =
    satisfy(Character.isJavaIdentifierPart) namedOpaque "java identifier part"

  private[parser] val parseJavaIdentifier: Parser[String] =
    (for {
      firstChar <- parseJavaIdentifierStart
      rest <- many(parseJavaIdentifierPart)
    } yield firstChar + rest.mkString) namedOpaque "java identifier"

  private[parser] val parseFqJavaIdentifier: Parser[String] =
    sepBy(parseJavaIdentifier, dot).map(_.mkString("."))

  private[parser] val parseJavaIdentifiers: Parser[(String, String, List[String])] =
    for {
      first <- parseJavaIdentifier <~ dot
      second <- parseJavaIdentifier
      rest <- many(dot ~> parseJavaIdentifier)
    } yield (first, second, rest)

  private[parser] val parseJavaNumeric: Parser[String] =
    many(digit).map(_.mkString(""))

  private[parser] val parseJavaString: Parser[String] =
    (quote ~> many(notChar('"')) <~ quote).map(_.mkString("\"", "", "\""))

  // FIXME This is a really basic parser for values
  // a lot can be improved here.
  private[parser] val parseJavaValue: Parser[String] =
    parseJavaString | parseJavaNumeric | err[String]("Unable to parse a value")

  private[parser] val parseParamWithoutValue: Parser[NormalParameter] =
    for {
      paramName <- skipWhitespace ~> parseJavaIdentifier <~ skipWhitespace
      paramType <- skipWhitespace ~> colon ~> skipWhitespace ~> parseFqJavaIdentifier <~ skipWhitespace
    } yield NormalParameter(paramName, paramType)

  private[parser] val parseDefaultParam: Parser[DefaultValueParameter] =
    for {
      paramName    <- skipWhitespace ~> parseJavaIdentifier <~ skipWhitespace
      paramType    <- skipWhitespace ~> colon ~> skipWhitespace ~> parseFqJavaIdentifier <~ skipWhitespace
      defaultValue <- skipWhitespace ~> qmark ~> equal ~> skipWhitespace ~> parseJavaValue <~ skipWhitespace
    } yield DefaultValueParameter(paramName, paramType, defaultValue)

  private[parser] val parseFixedParam: Parser[FixedValueParameter] =
    for {
      paramName    <- skipWhitespace ~> parseJavaIdentifier <~ skipWhitespace
      paramType    <- skipWhitespace ~> colon ~> skipWhitespace ~> parseFqJavaIdentifier <~ skipWhitespace
      defaultValue <- skipWhitespace ~> equal ~> skipWhitespace ~> parseJavaValue <~ skipWhitespace
    } yield FixedValueParameter(paramName, paramType, defaultValue)

  private[parser] val parseParamValue: Parser[Parameter] =
    parseDefaultParam      .map(_.asInstanceOf[Parameter]) |
    parseFixedParam        .map(_.asInstanceOf[Parameter]) |
    parseParamWithoutValue .map(_.asInstanceOf[Parameter]) |
    err[Parameter]("Unable to parse the parameter list")

  private[parser] val parseParams: Parser[List[Parameter]] =
    opt(parens(sepBy(parseParamValue, comma))).map {
      case Some(list) => list
      case None => Nil
    }

  private[parser] val parseCall: Parser[Call] =
    for {
      javaIdentifiers <- parseJavaIdentifiers
      params <- parseParams
    } yield toCall(javaIdentifiers, params)

  private def toCall(javaIdentifiers: (String, String, List[String]), params: List[Parameter]): Call = {
    val list = javaIdentifiers._1 :: javaIdentifiers._2 :: javaIdentifiers._3
    Call(toPackage(list.init.init), list.init.last, list.last, params)
  }

  private def toPackage(list: List[String]): Option[String] =
    if(list.isEmpty)
      None
    else
      Some(list.mkString("."))

  private[parser] val parseRouteEntry: Parser[RouteEntry] =
    (for {
      method <- parseHttpMethod <~ skipWhitespace
      url    <- parseUrlSegment <~ skipWhitespace
      call   <- parseCall <~ opt(skipWhitespace ~> semicolon <~ skipWhitespace)
    } yield RouteEntry(method, url, call)) named "routes entry"

  private[parser] val parseRouteFileReference: Parser[RouteFileReference] =
    (for {
      mountPoint <- string("->") ~> parseUrlSegment <~ skipWhitespace
      rest <- takeText
    } yield RouteFileReference(mountPoint, rest)) named "routes file reference"

  private[parser] val httpRouteParser: Parser[RouteFileEntry] =
    parseRouteFileReference .map(_.asInstanceOf[RouteFileEntry]) |
    parseRouteEntry         .map(_.asInstanceOf[RouteFileEntry]) |
    err[RouteFileEntry]("Unable to parse into a valid routes line")

  def parseLine(line: String): Either[String, Option[RouteFileEntry]] =
    if(line.trim.isEmpty)
      Right(None)
    else if(line.startsWith("#"))
      Right(None)
    else
      httpRouteParser
        .parseOnly(line)
        .either
        .right
        .map(x => Some(x))

  def parseLines(line: List[String]): Either[List[String], List[RouteFileEntry]] =
    listToEither(line.map(parseLine)).right.map(list => list.collect {
      case Some(x) => x
    })

  def parseFile(file: File): Either[List[String], List[RouteFileEntry]]  =
    parseLines(scala.io.Source.fromFile(file).getLines().toList)


  private def listToEither[A, B](eithers: List[Either[A, B]]): Either[List[A], List[B]] =
    eithers.partition(_.isLeft) match {
      case (Nil, rights) => Right(for(Right(b) <- rights) yield b)
      case (lefts, _)    => Left (for(Left(a)  <- lefts ) yield a)
    }

}
