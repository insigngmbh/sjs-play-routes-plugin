lazy val `sbt-scalajs-play` = (project in file("."))
  .settings(
    sbtPlugin := true,
    name := "sbt-scalajs-play-routes",
    organization := "ch.insign",
    version := "0.0.1-SNAPSHOT",
    libraryDependencies ++= Seq(
        "org.tpolecat" %% "atto-core"   % "0.5.2",
        "org.specs2"   %% "specs2-core" % "3.8.9" % "test"
    ),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.15")
  )