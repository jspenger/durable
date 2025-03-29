// format: off
val scala33               = "3.3.4"
val junitInterfaceVersion = "0.11"
val upickleVersion        = "3.1.0"
val sporksVersion         = "0.1.0-SNAPSHOT"

ThisBuild / organization     := "com.jspenger"
ThisBuild / organizationName := "Jonas Spenger"

ThisBuild / description := "Durable and fault tolerant computation library for Scala 3 with workflows and futures."
ThisBuild / licenses    := List("Apache-2.0" -> new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

ThisBuild / scalaVersion := scala33
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / developers := List(
  Developer(
    id    = "jspenger",
    name  = "Jonas Spenger",
    email = "@jonasspenger",
    url   = url("https://github.com/jspenger")
  )
)
// format: on

lazy val root = project
  .in(file("durable-root"))
  .settings(
    name := "durable",
    resolvers += "jitpack" at "https://jitpack.io",
    // libraryDependencies += "com.github.jspenger" % "sporks3" % "main-SNAPSHOT",
    libraryDependencies += "com.jspenger" %% "sporks3" % sporksVersion,
    libraryDependencies += "com.lihaoyi" %% "upickle" % upickleVersion,
    libraryDependencies += "com.novocode" % "junit-interface" % junitInterfaceVersion % Test,
  )

lazy val example = project
  .in(file("durable-example"))
  .settings(
    name := "example",
    publish / skip := true,
  )
  .dependsOn(root)
