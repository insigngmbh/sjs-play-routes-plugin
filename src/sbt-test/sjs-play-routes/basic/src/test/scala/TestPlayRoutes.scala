import utest._

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.Protocol.HTTP
import fr.hmil.roshttp.Protocol.HTTPS
import fr.hmil.roshttp.response.SimpleHttpResponse
import fr.hmil.roshttp.exceptions.HttpException

import scalajs.concurrent.JSExecutionContext.Implicits.queue
// import scalajs.concurrent.JSExecutionContext.Implicits.runNow

import routes.ch.insign.{Blog, Website}

object TestPlayRoutes extends TestSuite {

  implicit val client =
    HttpRequest()
      .withProtocol(HTTPS)
      .withHost("jsonplaceholder.typicode.com")
      .withPort(443)

  private def checkHttpResponse(expectedStatus: Int)(response: SimpleHttpResponse): Boolean =
    if(response.statusCode == expectedStatus)
      true
    else
      throw new IllegalStateException(s"Wrong status code: ${response.statusCode}")

  val tests = this {

    'root {
      Blog.index().send().map(checkHttpResponse(200))
    }

    'posts {
      Blog.posts().send().map(checkHttpResponse(200))
    }

    'defaultParam {
      Blog.defaultParam().send().map(checkHttpResponse(200))
    }

    'post1 {
      Blog.post(1).send().map(checkHttpResponse(200))
    }

    'addPost {
      Blog.addPost().send().map(checkHttpResponse(201))
    }

    'editPost {
      Blog.editPost(1).send().map(checkHttpResponse(200))
    }

    'editPost {
      Blog.deletePost(1).send().map(checkHttpResponse(200))
    }

    'post1comments {
      Blog.comments(1).send().map(checkHttpResponse(200))
    }

    'albums {
      Website.page("foobar").send().map(checkHttpResponse(200))
    }

    'users {
      Website.page("users").send().map(checkHttpResponse(200))
    }

    /*
    'regex {
      Website.regex(1).map(checkHttpResponse(200))
    }
    */

  }

  tests.runAsync().map { results =>
    assert(results.toSeq(0).value.isSuccess)  // GET    /
    assert(results.toSeq(1).value.isSuccess)  // GET    /posts
    assert(results.toSeq(2).value.isSuccess)  // GET    /posts?userId=1
    assert(results.toSeq(3).value.isSuccess)  // GET    /post/1
    assert(results.toSeq(4).value.isSuccess)  // POST   /posts
    assert(results.toSeq(5).value.isSuccess)  // PUT    /posts/1
    assert(results.toSeq(6).value.isSuccess)  // DELETE /posts/1
    assert(results.toSeq(7).value.isSuccess)  // GET    /post/1/comments
    assert(results.toSeq(8).value.isSuccess)  // GET    /albums
    assert(results.toSeq(9).value.isSuccess)  // GET    /users
  }

}