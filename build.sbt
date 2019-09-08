val scala211 = "2.11.12"
val scala212 = "2.12.8"
val supportedScalaVersions = List(scala212, scala211)

ThisBuild / scalaVersion := scala212
ThisBuild / organization := "goedelsoup"

val CatsVersion = "2.0.0-RC1"
val CatsEffectVersion = "2.0.0-RC1"
val CirceVersion = "0.11.1"
val CirisVersion = "0.12.1"
val DisciplineSpecs2Version = "1.0.0-RC1"
val FUUIDVersion = "0.2.0"
val FS2Version = "1.1.0-M2"
val Http4sVersion = "0.20.3"
val Log4CatsVersion = "0.3.0"
val MulesVersion = "0.2.0"
val RocksDBVersion = "5.5.1"
val Specs2Version = "4.7.0"

addCommandAlias("fmtAll", ";scalafmt; test:scalafmt; scalafmtSbt")
addCommandAlias("fmtCheck", ";scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings",
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val publishSettings = Seq(
  crossScalaVersions := supportedScalaVersions,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  releaseCrossBuild := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  bintrayOrganization in bintray := None,
  pomIncludeRepository := { _ =>
    false
  },
)

lazy val core = (project in file("core"))
  .settings(
    name := "rocks4s-core",
    libraryDependencies ++= Seq(
      "org.rocksdb" % "rocksdbjni" % RocksDBVersion,
      "org.typelevel" %% "cats-core" % CatsVersion,
      "org.typelevel" %% "cats-free" % CatsVersion,
      "org.typelevel" %% "cats-kernel" % CatsVersion,
      "org.typelevel" %% "cats-macros" % CatsVersion,
      "co.fs2" %% "fs2-core" % FS2Version,
      "co.fs2" %% "fs2-io" % FS2Version,
      "io.chrisdavenport" %% "log4cats-core" % Log4CatsVersion % Test,
      "io.chrisdavenport" %% "log4cats-slf4j" % Log4CatsVersion % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
      "ch.qos.logback" % "logback-core" % "1.2.3" % Test,
      "io.chrisdavenport" %% "fuuid" % FUUIDVersion % Test,
      "org.typelevel" %% "cats-effect-laws" % CatsEffectVersion % Test,
      "org.specs2" %% "specs2-core" % Specs2Version % Test,
      "org.specs2" %% "specs2-scalacheck" % Specs2Version % Test,
      "org.typelevel" %% "discipline-specs2" % DisciplineSpecs2Version % Test,
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0"),
    scalafmtOnCompile := true,
    initialCommands in console :=
      """
        |import cats.effect._
        |import cats.implicits._
        |
        |import rocks4s._
        |
        |implicit val CS = IO.contextShift(scala.concurrent.ExecutionContext.global)
      """.stripMargin
  )
  .settings(publishSettings)

lazy val circe = (project in file("circe"))
  .settings(
    name := "rocks4s-circe",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.typelevel" %% "cats-effect-laws" % CatsEffectVersion % Test,
      "org.specs2" %% "specs2-core" % Specs2Version % Test,
      "org.specs2" %% "specs2-scalacheck" % Specs2Version % Test,
      "org.typelevel" %% "discipline-specs2" % DisciplineSpecs2Version % Test,
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0"),
    scalafmtOnCompile := true,
  )
  .settings(publishSettings)
  .dependsOn(core)

lazy val mules = (project in file("mules"))
  .settings(
    name := "rocks4s-mules",
    libraryDependencies ++= Seq(
      "io.chrisdavenport" %% "mules" % MulesVersion,
      "org.typelevel" %% "cats-effect-laws" % CatsEffectVersion % Test,
      "org.specs2" %% "specs2-core" % Specs2Version % Test,
      "org.specs2" %% "specs2-scalacheck" % Specs2Version % Test,
      "org.typelevel" %% "discipline-specs2" % DisciplineSpecs2Version % Test,
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0"),
    scalafmtOnCompile := true,
  )
  .settings(publishSettings)
  .dependsOn(core)

lazy val examples = (project in file("examples"))
  .settings(
    name := "rocks4s-examples",
    libraryDependencies ++= Seq(
      "is.cir" %% "ciris-cats" % CirisVersion,
      "is.cir" %% "ciris-cats-effect" % CirisVersion,
      "is.cir" %% "ciris-core" % CirisVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.chrisdavenport" %% "log4cats-core" % Log4CatsVersion,
      "io.chrisdavenport" %% "log4cats-slf4j" % Log4CatsVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "ch.qos.logback" % "logback-core" % "1.2.3",
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0"),
    scalafmtOnCompile := true,
  )
  .settings(noPublishSettings)
  .dependsOn(circe, core, mules)

val root = (project in file("."))
  .settings(noPublishSettings)
  .aggregate(circe, core, mules, examples)
  .dependsOn(circe, core, mules, examples)