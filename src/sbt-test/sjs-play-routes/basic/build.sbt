lazy val root = (project in file("."))
  .settings(
    scalajsPlayRoutesFile := "routes",
    libraryDependencies += "com.lihaoyi" %%% "utest" % "0.4.5" % "test",
    testFrameworks += new TestFramework("utest.runner.Framework"),
    scalacOptions += "-deprecation"
  )
  .enablePlugins(ScalajsPlayRoutes)