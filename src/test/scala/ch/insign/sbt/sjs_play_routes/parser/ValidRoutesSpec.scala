package ch.insign.sbt.sjs_play_routes.parser

import org.specs2._

import scala.io.Source
import scala.tools.nsc.interpreter.InputStream
import ch.insign.sbt.sjs_play_routes.parser.RoutesParser.parseLines

class ValidRoutesSpec extends Specification { def is = s2"""

  This is a specification for the routes parser

  The valid routes file should
    contain the correct routes                $testParseRoutes
    contain the Blog.index route              $testExampleIndex
    contain the Blog.posts route              $testExamplePosts
    contain the Blog.defaultParam route       $testExampleDefaultParam
    contain the Blog.addPost route            $testAddPost
    contain the Blog.post route               $testGetPost
    contain the Blog.editPost route           $testEditPost
    contain the Blog.deletePost route         $testDeletePost
    contain the Blog.comments route           $testComments
    contain the Pages.page route (static)     $testStaticPage
    contain the Pages.page route (variable)   $testVariablePage

"""

  private lazy val inputStream: InputStream =
    getClass.getClassLoader.getResourceAsStream("valid.routes")

  private lazy val fileLines: List[String] =
    Source.fromInputStream(inputStream).getLines().toList

  private lazy val parsedRoutes: Either[List[String], List[RouteFileEntry]] =
    parseLines(fileLines)

  private def testParseRoutes() =
    parsedRoutes.right.map(_.size) must beRight(10)

  private def testExampleIndex() =
    parsedRoutes.right.map(_.head) must beRight(
      RouteEntry(
        GET,
        "/",
        Call(
          Some("ch.insign"),
          "Blog",
          "index"
        )
      )
    )

  private def testExamplePosts() =
    parsedRoutes.right.map(_.apply(1)) must beRight(
      RouteEntry(
        GET,
        "/posts",
        Call(
          Some("ch.insign"),
          "Blog",
          "posts"
        )
      )
    )

  private def testExampleDefaultParam() =
    parsedRoutes.right.map(_.apply(2)) must beRight(
      RouteEntry(
        GET,
        "/default_param",
        Call(
          Some("ch.insign"),
          "Blog",
          "defaultParam",
          List(
            DefaultValueParameter("userId", "Int", "1")
          )
        )
      )
    )

  private def testAddPost() =
    parsedRoutes.right.map(_.apply(3)) must beRight(
      RouteEntry(
        POST,
        "/posts",
        Call(
          Some("ch.insign"),
          "Blog",
          "addPost"
        )
      )
    )

  private def testGetPost() =
    parsedRoutes.right.map(_.apply(4)) must beRight(
      RouteEntry(
        GET,
        "/posts/:id",
        Call(
          Some("ch.insign"),
          "Blog",
          "post",
          List(
            NormalParameter("id", "Int")
          )
        )
      )
    )

  private def testEditPost() =
    parsedRoutes.right.map(_.apply(5)) must beRight(
      RouteEntry(
        PUT,
        "/posts/:id",
        Call(
          Some("ch.insign"),
          "Blog",
          "editPost",
          List(
            NormalParameter("id", "Int")
          )
        )
      )
    )


  private def testDeletePost() =
    parsedRoutes.right.map(_.apply(6)) must beRight(
      RouteEntry(
        DELETE,
        "/posts/:id",
        Call(
          Some("ch.insign"),
          "Blog",
          "deletePost",
          List(
            NormalParameter("id", "Int")
          )
        )
      )
    )

  private def testComments() =
    parsedRoutes.right.map(_.apply(7)) must beRight(
      RouteEntry(
        GET,
        "/posts/$id/comments",
        Call(
          Some("ch.insign"),
          "Blog",
          "comments",
          List(
            NormalParameter("id", "Int")
          )
        )
      )
    )

  private def testStaticPage() =
    parsedRoutes.right.map(_.apply(8)) must beRight(
      RouteEntry(
        GET,
        "/albums",
        Call(
          Some("ch.insign"),
          "Pages",
          "page",
          List(
            FixedValueParameter("page", "String", "\"albums\"")
          )
        )
      )
    )

  private def testVariablePage() =
    parsedRoutes.right.map(_.apply(9)) must beRight(
      RouteEntry(
        GET,
        "/*page",
        Call(
          Some("ch.insign"),
          "Pages",
          "page",
          List(
            NormalParameter("page", "String")
          )
        )
      )
    )

}
