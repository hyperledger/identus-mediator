resolvers ++= Resolver.sonatypeOssRepos("public")
resolvers ++= Resolver.sonatypeOssRepos("snapshots")

inThisBuild(
  Seq(
    scalaVersion := "3.2.2", // Also update docs/publishWebsite.sh and any ref to scala-3.2.2
  )
)

/** Versions */
lazy val V = new {
  val scalaDID = "0.1.0-M2"
//   val scalajsJavaSecureRandom = "1.0.0"

  // FIXME another bug in the test framework https://github.com/scalameta/munit/issues/554
  val munit = "1.0.0-M7" // "0.7.29"

//   // https://mvnrepository.com/artifact/org.scala-js/scalajs-dom
//   val scalajsDom = "2.4.0"
//   // val scalajsLogging = "1.1.2-SNAPSHOT" //"1.1.2"

//   // https://mvnrepository.com/artifact/dev.zio/zio
//   val zio = "2.0.13"
//   val zioJson = "0.4.2"
  // val zioMunitTest = "0.1.1"
  val zioHttp = "0.0.5"
//   val zioPrelude = "1.0.0-RC19"

//   // https://mvnrepository.com/artifact/io.github.cquiroz/scala-java-time
//   val scalaJavaTime = "2.3.0"

//   val logbackClassic = "1.2.10"
//   val scalaLogging = "3.9.4"

//   val laika = "0.19.1"

//   val laminar = "15.0.1"
//   val waypoint = "6.0.0"
//   val upickle = "3.1.0"
//   // https://www.npmjs.com/package/material-components-web
//   val materialComponents = "12.0.0"
}

/** Dependencies */
lazy val D = new {
  val scalaDID = Def.setting("app.fmgp" %%% "did" % V.scalaDID)
  val scalaDID_imp = Def.setting("app.fmgp" %%% "did-imp" % V.scalaDID)
  val scalaDID_peer = Def.setting("app.fmgp" %%% "did-method-peer" % V.scalaDID)

//   /** The [[java.security.SecureRandom]] is used by the [[java.util.UUID.randomUUID()]] method in [[MsgId]].
//     *
//     * See more https://github.com/scala-js/scala-js-java-securerandom
//     */
//   val scalajsJavaSecureRandom = Def.setting(
//     ("org.scala-js" %%% "scalajs-java-securerandom" % V.scalajsJavaSecureRandom)
//       .cross(CrossVersion.for3Use2_13)
//   )

//   val dom = Def.setting("org.scala-js" %%% "scalajs-dom" % V.scalajsDom)

//   val zio = Def.setting("dev.zio" %%% "zio" % V.zio)
//   val zioStreams = Def.setting("dev.zio" %%% "zio-streams" % V.zio)
//   val zioJson = Def.setting("dev.zio" %%% "zio-json" % V.zioJson)
  val ziohttp = Def.setting("dev.zio" %% "zio-http" % V.zioHttp)
//   val zioPrelude = Def.setting("dev.zio" %%% "zio-prelude" % V.zioPrelude)
//   // val zioTest = Def.setting("dev.zio" %%% "zio-test" % V.zio % Test)
//   // val zioTestSBT = Def.setting("dev.zio" %%% "zio-test-sbt" % V.zio % Test)
//   // val zioTestMagnolia = Def.setting("dev.zio" %%% "zio-test-magnolia" % V.zio % Test)
//   val zioMunitTest = Def.setting("com.github.poslegm" %%% "munit-zio" % V.zioMunitTest % Test)

//   // Needed for ZIO
//   val scalaJavaT = Def.setting("io.github.cquiroz" %%% "scala-java-time" % V.scalaJavaTime)
//   val scalaJavaTZ = Def.setting("io.github.cquiroz" %%% "scala-java-time-tzdb" % V.scalaJavaTime)

//   // Test DID comm
//   // val didcomm = Def.setting("org.didcommx" % "didcomm" % "0.3.1")

//   // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = Def.setting("org.scalameta" %%% "munit" % V.munit % Test)

//   val laika = Def.setting("org.planet42" %%% "laika-core" % V.laika) // JVM & JS

//   // For WEBAPP
//   val laminar = Def.setting("com.raquo" %%% "laminar" % V.laminar)
//   val waypoint = Def.setting("com.raquo" %%% "waypoint" % V.waypoint)
//   val upickle = Def.setting("com.lihaoyi" %%% "upickle" % V.upickle)
}

inThisBuild(
  Seq(
    scalacOptions ++= Seq(
      // ### https://docs.scala-lang.org/scala3/guides/migration/options-new.html
      // ### https://docs.scala-lang.org/scala3/guides/migration/options-lookup.html
      //
      "-encoding", // if an option takes an arg, supply it on the same line
      "UTF-8", // source files are in UTF-8
      "-deprecation", // warn about use of deprecated APIs
      "-unchecked", // warn about unchecked type parameters
      "-feature", // warn about misused language features (Note we are using 'language:implicitConversions')
      "-Xfatal-warnings",
      // TODO "-Yexplicit-nulls",
      // "-Ysafe-init", // https://dotty.epfl.ch/docs/reference/other-new-features/safe-initialization.html
      "-language:implicitConversions", // we can use with the flag '-feature'
      // NO NEED ATM "-language:reflectiveCalls",
      // "-Xprint-diff",
      // "-Xprint-diff-del",
      // "-Xprint-inline",
      // NO NEED ATM "-Xsemanticdb"
      // NO NEED ATM "-Ykind-projector"
    ),

    // ### commonSettings ###
    // Compile / doc / sources := Nil,
    // ### setupTestConfig ### //lazy val settingsFlags: Seq[sbt.Def.SettingsDefinition] = ???
    // libraryDependencies += D.munit.value, // BUG? "JS's Tests does not stop"
  )
)

lazy val setupTestConfig: Seq[sbt.Def.SettingsDefinition] = Seq(
  libraryDependencies += D.munit.value,
)

lazy val scalaJSBundlerConfigure: Project => Project =
  _.enablePlugins(ScalaJSPlugin)
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      scalaJSLinkerConfig ~= {
        _.withSourceMap(false) // disabled because it somehow triggers warnings and errors
          .withModuleKind(ModuleKind.CommonJSModule) // ModuleKind.ESModule
          // must be set to ModuleKind.CommonJSModule in projects where ScalaJSBundler plugin is enabled
          .withJSHeader(
            """/* FMGP scala-did examples and tool
            | * https://github.com/FabioPinheiro/scala-did
            | * Copyright: Fabio Pinheiro - fabiomgpinheiro@gmail.com
            | */""".stripMargin.trim() + "\n"
          )
      }
    )
    // .settings( //TODO https://scalacenter.github.io/scalajs-bundler/reference.html#jsdom
    //   //jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    //   //Test / requireJsDomEnv := true)
    // )
    .enablePlugins(ScalablyTypedConverterPlugin)
    .settings(
      // Compile / fastOptJS / webpackExtraArgs += "--mode=development",
      // Compile / fullOptJS / webpackExtraArgs += "--mode=production",
      Compile / fastOptJS / webpackDevServerExtraArgs += "--mode=development",
      Compile / fullOptJS / webpackDevServerExtraArgs += "--mode=production",
      useYarn := true
    )

lazy val buildInfoConfigure: Project => Project = _.enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "fmgp",
    // buildInfoObject := "BuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildTime") { System.currentTimeMillis }, // re-computed each time at compile
    ),
  )

// lazy val core = RootProject(file(".."))

lazy val httpUtils = crossProject(JSPlatform, JVMPlatform) // project
  .in(file("http-utils"))
  .settings(publish / skip := true)
  .settings(
    libraryDependencies += D.scalaDID.value,
  )
  .jvmSettings(
    libraryDependencies += D.ziohttp.value,
  )

lazy val mediator = project
  .in(file("did-mediator"))
  .settings(publish / skip := true)
  .settings(
    libraryDependencies += D.scalaDID_imp.value,
    libraryDependencies += D.scalaDID_peer.value,
    libraryDependencies += D.ziohttp.value,
  )
  .settings(
    Compile / mainClass := Some("fmgp.did.demo.MediatorStandalone"),
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerUsername := Some("input-output-hk"),
    Docker / dockerRepository := Some("ghcr.io"),
    dockerExposedPorts := Seq(8080),
    dockerBaseImage := "openjdk:11",
  )
  .dependsOn(httpUtils.jvm) // did, didExample,

// ############################
// ####  Release process  #####
// ############################
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.*
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  ReleaseStep(releaseStepTask(mediator / Docker / stage)),
  setNextVersion
)
