# sbt-scalajs-play-routes

A Routes generator for scalajs for Play routes files.

## Motivation

The [Play! Framework](https://playframework.com/) provides a typesafe way to define
[routes in configuration files](https://www.playframework.com/documentation/2.5.x/JavaRouting).

By parsing this configuration, one can easily derive a client which calls those routes.

## Usage

### Adding the plugin to the build

*project/plugins.sbt*

```scala
// Add the insign bintray repo
resolvers += Resolver.bintrayIvyRepo("insign", "sbt-plugins")
// Add the routes plugin
addSbtPlugin("ch.insign" % "sbt-sjs-play-routes" % "0.1.0")
```

*build.sbt*

```scala
lazy val client = (project in file("client"))
  .settings(
    scalajsPlayRoutesFile := "server/conf/routes"
    // more settings, ...
  )
  .enablePlugins(ScalajsPlayRoutes)
```

### Define routes in the configuration

*server/conf/routes*

```text
GET     /todos                      controllers.TodoController.list
GET     /todos/:id                  controllers.TodoController.show(id: Int)
POST    /todos/create/              controllers.TodoController.create()
PUT     /todos/:id                  controllers.TodoController.update(id: Int)
DELETE  /todos/:id                  controllers.TodoController.delete(id: Int)
```

### Using the routes in the code

```scala
// Library imports
import fr.hmil.roshttp.{HttpRequest, Protocol}
import fr.hmil.roshttp.response.SimpleHttpResponse
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// Import the generated client
import routes.controllers.TodoController

// Create a httpRequest (it will be reused for every request and passed implicitly)
implicit private val httpRequest =
  HttpRequest()
    .withProtocol(Protocol.HTTP)
    .withHost("localhost")
    .withPort(9000)

// A GET request
def list(): Future[SimpleHttpResponse] =
  TodoController
    .list()
    .send()

// A GET request with a parameter
def show(id: Int): Future[SimpleHttpResponse] =
  TodoController
    .show(id)
    .send()

// A POST request with plaintext body data
def create(data: String): Future[SimpleHttpResponse] =
  TodoController
    .create()
    .send(PlainTextBody(data))
    
// A PUT request with plaintext body data
def update(id: Int, data: String): Future[SimpleHttpResponse] =
  TodoController
    .update(id)
    .send(PlainTextBody(data))
    
// A DELETE request
def delete(id: Int): Future[SimpleHttpResponse] =
  TodoController
    .delete(id)
    .send()
```

## Tasks & Settings

**Tasks**

`scalajsPlayRoutes` - Launches the routes compiler.

**Settings**

`scalajsPlayRoutesFile` (required) - Defines the location of the `routes` file.

`scalajsPlayRoutesPrefix` (optional; default = `"routes"`) - The package of the generated files can be prefixed.


## Libraries

As a HTTP client, [RösHTTP](https://github.com/hmil/RosHTTP) will be added to the libraryDependencies.

## Demo

A full REST example (using JSON with [circe](https://circe.github.io/circe/)) can be found in the
[demo project](https://github.com/insigngmbh/sjs-play-routes-demo/blob/master/client/src/main/scala/ch/insign/client/TodoClient.scala).

## Q & A

##### Why not autowire?

[Autowire](https://github.com/lihaoyi/autowire) is great, so why not use that?

We work with Play-Java, which sadly doesn't make it easy to use Autowire.
Our devs spent a while trying to set it up, but it never really worked well.

##### Are references supported

Play supports importing routes from other files using the special `->` syntax.
This is currently not supported.

## Bugs, Feedback, Questions, Improvements, etc

Feel free to create an issue or send us your PR!

## Shout-outs

This little project wouldn't be possible without the works of:
- [Scala](http://scala-lang.org/)
- [SBT](http://www.scala-sbt.org)
- [Scala.js](http://www.scala-js.org/)
- [Play! Framework](https://playframework.com/)
- [RösHTTP](https://github.com/hmil/RosHTTP)
- [Atto](https://github.com/tpolecat/atto)
- [Specs2](https://etorreborre.github.io/specs2/)
