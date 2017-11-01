lazy val `sbt-scalajs-play` = (project in file("."))
  .settings(
    sbtPlugin := true,
    name := "sbt-sjs-play-routes",
    description := "A compiler for Play! Routes files",
    organization := "ch.insign",
    version := "0.0.1-SNAPSHOT",
    libraryDependencies ++= Seq(
        "org.tpolecat" %% "atto-core"   % "0.5.2",
        "org.specs2"   %% "specs2-core" % "3.8.9" % "test"
    ),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.15"),
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    publishMavenStyle := false,
    bintrayRepository := "sbt-plugins",
    bintrayOrganization := Some("insign")
  )