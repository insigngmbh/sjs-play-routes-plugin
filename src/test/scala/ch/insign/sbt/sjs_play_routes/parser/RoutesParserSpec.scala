package ch.insign.sbt.sjs_play_routes.parser

import atto.Atto._
import atto.compat.stdlib._
import ch.insign.sbt.sjs_play_routes.parser.RoutesParser._
import org.specs2._

class RoutesParserSpec extends Specification { def is = s2"""

  This is a specification for the routes parser

  The parser should
    be able to parse the GET method           $testParseHttpMethodGET
    be able to parse the PUT method           $testParseHttpMethodPUT
    be able to parse the POST method          $testParseHttpMethodPOST
    be able to parse the DELETE method        $testParseHttpMethodDELETE
    be able to parse the OPTIONS method       $testParseHttpMethodOPTIONS
    be able to parse the PATCH method         $testParseHttpMethodPATCH

    be able to parse an URL segment           $testParseUrlSegment

    be able to parse a controller and method  $testParseControllerAndMethod
    be able to parse a call with a package    $testParsePackageControllerAndMethod

    be able to parse empty parens             $testParseNoParam

    be able to parse a parameter              $testParseNoValueParam
    be able to parse many parameters          $testParseManyNoValueParams

    be able to parse a default parameter      $testParseDefaultValueParam
    be able to parse many default parameters  $testParseManyDefaultValueParams

"""

  private def testParseHttpMethodGET =
    parseHttpMethod.parseOnly("GET").either must beRight(GET)

  private def testParseHttpMethodPUT =
    parseHttpMethod.parseOnly("PUT").either must beRight(PUT)

  private def testParseHttpMethodPOST =
    parseHttpMethod.parseOnly("POST").either must beRight(POST)

  private def testParseHttpMethodDELETE =
    parseHttpMethod.parseOnly("DELETE").either must beRight(DELETE)

  private def testParseHttpMethodOPTIONS =
    parseHttpMethod.parseOnly("OPTIONS").either must beRight(OPTIONS)

  private def testParseHttpMethodPATCH =
    parseHttpMethod.parseOnly("PATCH").either must beRight(PATCH)

  private def testParseUrlSegment =
    parseUrlSegment.parseOnly("/foo/$$id/:foobaz/blah").either must beRight("/foo/$$id/:foobaz/blah")

  private def testParseControllerAndMethod =
    parseCall.parseOnly("FooBarController.doStuff").either must beRight(Call(None, "FooBarController", "doStuff", List()))

  private def testParsePackageControllerAndMethod =
    parseCall.parseOnly("xyz._0x7e.FooBarController.doStuff").either must beRight(Call(Some("xyz._0x7e"), "FooBarController", "doStuff", List()))

  private def testParseNoParam =
    parseParams.parseOnly("()").either must beRight[List[Parameter]](Nil)

  private def testParseNoValueParam =
    parseParams.parseOnly("(id: Int)").either must beRight[List[Parameter]](
      List(
        NormalParameter("id", "Int")
      )
    )

  private def testParseManyNoValueParams =
    parseParams.parseOnly("(id: Int, foo: String, bar: Long)").either must beRight[List[Parameter]](
      List(
        NormalParameter("id", "Int"),
        NormalParameter("foo", "String"),
        NormalParameter("bar", "Long")
      )
    )

  private def testParseDefaultValueParam =
    parseDefaultParam.parseOnly("id: Int ?= 0").either must beRight[DefaultValueParameter](DefaultValueParameter("id", "Int", "0"))

  private def testParseManyDefaultValueParams =
    parseParams.parseOnly("""(id: Int ?= 1, name: String ?= "timo")""").either must beRight[List[Parameter]](
      List(
        DefaultValueParameter("id", "Int", "1"),
        DefaultValueParameter("name", "String", "\"timo\"")
      )
    )

}
