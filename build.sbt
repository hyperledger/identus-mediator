inThisBuild(
  Seq(
    organization := "app.fmgp",
    scalaVersion := "3.1.3", // Also update docs/publishWebsite.sh and any ref to scala-3.1.3
    updateOptions := updateOptions.value.withLatestSnapshots(false),
  )
)

lazy val docs = project // new documentation project
  .in(file("docs-build")) // important: it must not be docs/
  .settings(
    mdocJS := Some(webapp),
    // https://scalameta.org/mdoc/docs/js.html#using-scalajs-bundler
    mdocJSLibraries := ((webapp / Compile / fullOptJS) / webpack).value,
    mdoc := {
      val log = streams.value.log
      (mdoc).evaluated
      scala.sys.process.Process("pwd") ! log
      scala.sys.process.Process(
        "md2html" :: "docs-build/target/mdoc/readme.md" :: Nil
      ) #> file("docs-build/target/mdoc/readme.html") ! log
    }
  )
  .settings(mdocVariables := Map("VERSION" -> version.value))
  .dependsOn(webapp) // jsdocs)
  .enablePlugins(MdocPlugin) // , DocusaurusPlugin)

/** Versions */
lazy val V = new {

  // FIXME another bug in the test framework https://github.com/scalameta/munit/issues/554
  val munit = "1.0.0-M6" // "0.7.29"

  // https://mvnrepository.com/artifact/org.scala-js/scalajs-dom
  val scalajsDom = "2.0.0" // scalajsDom 2.0.0 need to update sbt-converter to 37?
  // val scalajsLogging = "1.1.2-SNAPSHOT" //"1.1.2"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.0"
  val zioJson = "0.3.0-RC10"

  // https://mvnrepository.com/artifact/io.github.cquiroz/scala-java-time
  val scalaJavaTime = "2.3.0"

  val logbackClassic = "1.2.10"
  val scalaLogging = "3.9.4"

  val laminar = "0.14.2"
  val waypoint = "0.5.0"
  val upickle = "2.0.0"
  // https://www.npmjs.com/package/material-components-web
  val materialComponents = "12.0.0"
}

/** Dependencies */
lazy val D = new {
  val dom = Def.setting("org.scala-js" %%% "scalajs-dom" % V.scalajsDom)

  val zio = Def.setting("dev.zio" %%% "zio" % V.zio)
  val zioStreams = Def.setting("dev.zio" %%% "zio-streams" % V.zio)
  val zioJson = Def.setting("dev.zio" %%% "zio-json" % V.zioJson)

  // Needed for ZIO
  val scalaJavaT = Def.setting("io.github.cquiroz" %%% "scala-java-time" % V.scalaJavaTime)
  val scalaJavaTZ = Def.setting("io.github.cquiroz" %%% "scala-java-time-tzdb" % V.scalaJavaTime)

  // Test DID comm
  // val didcomm = Def.setting("org.didcommx" % "didcomm" % "0.3.1")

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = Def.setting("org.scalameta" %%% "munit" % V.munit % Test)

  // For WEBAPP
  val laminar = Def.setting("com.raquo" %%% "laminar" % V.laminar)
  val waypoint = Def.setting("com.raquo" %%% "waypoint" % V.waypoint)
  val upickle = Def.setting("com.lihaoyi" %%% "upickle" % V.upickle)
}

/** NPM Dependencies */
lazy val NPM = new {
  // https://www.npmjs.com/package/@types/d3
  // val d3NpmDependencies = Seq("d3", "@types/d3").map(_ -> "7.1.0")

  val mermaid = Seq("mermaid" -> "8.14.0", "@types/mermaid" -> "8.2.8")

  val materialDesign = Seq("material-components-web" -> V.materialComponents)

  val ipfsClient = Seq("multiformats" -> "9.6.4")

  // val nodeJose = Seq("node-jose" -> "2.1.1", "@types/node-jose" -> "1.1.10")
  // val elliptic = Seq("elliptic" -> "6.5.4", "@types/elliptic" -> "6.4.14")
  val jose = Seq("jose" -> "4.8.3") // , "ts-jose" -> "4.8.3")

  // val prism = Seq("@input-output-hk/atala-prism-sdk-test" -> "0.0.11")

}

lazy val noPublishSettings = skip / publish := true
lazy val publishSettings = {
  val repo = "https://github.com/FabioPinheiro/fmgp-generative-design"
  val contact = Developer("FabioPinheiro", "Fabio Pinheiro", "fabiomgpinheiro@gmail.com", url("http://fmgp.app"))
  Seq(
    Test / publishArtifact := false,
    pomIncludeRepository := (_ => false),
    homepage := Some(url(repo)),
    licenses := Seq("MIT License" -> url(repo + "/blob/master/LICENSE")),
    scmInfo := Some(ScmInfo(url(repo), "scm:git:git@github.com:FabioPinheiro/fmgp-generative-design.git")),
    developers := List(contact)
  )
}

lazy val settingsFlags: Seq[sbt.Def.SettingsDefinition] = Seq(
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8", // source files are in UTF-8
    "-deprecation", // warn about use of deprecated APIs
    "-unchecked", // warn about unchecked type parameters
    "-feature", // warn about misused language features
    "-Xfatal-warnings",
    // "-Yexplicit-nulls",
    // TODO "-Ysafe-init",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    // "-Xsource:3", //https://scalacenter.github.io/scala-3-migration-guide/docs/tooling/migration-tools.html
    // "-Ytasty-reader",
    "-Xprint-diff-del", // "-Xprint-diff",
    "-Xprint-inline",
  ) // ++ Seq("-rewrite", "-indent", "-source", "future-migration") //++ Seq("-source", "future")
)

lazy val setupTestConfig: Seq[sbt.Def.SettingsDefinition] = Seq(
  libraryDependencies += D.munit.value,
)
lazy val setupTestConfigJS: Seq[sbt.Def.SettingsDefinition] = Seq(
  // Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
)

lazy val commonSettings: Seq[sbt.Def.SettingsDefinition] = settingsFlags ++ Seq(
  Compile / doc / sources := Nil,
)

lazy val scalaJSBundlerConfigure: Project => Project =
  _.settings(commonSettings: _*)
    .enablePlugins(ScalaJSPlugin)
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings((setupTestConfig ++ setupTestConfigJS): _*)
    .settings(
      /* disabled because it somehow triggers many warnings */
      scalaJSLinkerConfig ~= (_.withSourceMap(false).withModuleKind(ModuleKind.CommonJSModule)),
      scalaJSLinkerConfig ~= {
        _.withJSHeader(
          """/* FMGP IPFS Example tool
            | * https://github.com/FabioPinheiro/did
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
    buildInfoPackage := "fmgp.ipfs",
    // buildInfoObject := "BuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildTime") { System.currentTimeMillis }, // re-computed each time at compile
    ),
  )

addCommandAlias("testJVM", ";didJVM/test")
addCommandAlias("testJS", ";didJS/test")
addCommandAlias("testAll", ";testJVM;testJS")

lazy val root = project
  .in(file("."))
  .aggregate(webapp, did.js, did.jvm)
  .settings(commonSettings: _*)
  .settings(noPublishSettings)

lazy val did = crossProject(JSPlatform, JVMPlatform)
  .in(file("did"))
  .settings((setupTestConfig ++ setupTestConfigJS): _*)
  .settings(
    name := "did",
    version := "0.1-SNAPSHOT",
    libraryDependencies += D.zioJson.value,
  )
  .jvmSettings( // Add JVM-specific settings here
    libraryDependencies += "org.bouncycastle" % "bcprov-jdk18on" % "1.71", // https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
    libraryDependencies += "org.bouncycastle" % "bcpkix-jdk18on" % "1.71", // https://mvnrepository.com/artifact/org.bouncycastle/bcpkix-jdk18on
    libraryDependencies += "com.nimbusds" % "nimbus-jose-jwt" % "9.23", // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt/9.23

    // Needed for nimbus-jose-jwt with Ed25519Signer
    // BUT have vulnerabilities in the dependencies: CVE-2022-25647
    libraryDependencies += "com.google.crypto.tink" % "tink" % "1.6.1", // https://mvnrepository.com/artifact/com.google.crypto.tink/tink/1.6.1
    // FIX vulnerabilitie https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-25647
    libraryDependencies += "com.google.code.gson" % "gson" % "2.9.0",
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.21.4",
  )
  // .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
  .jsConfigure(scalaJSBundlerConfigure)
  .jsSettings( // Add JS-specific settings here
    Compile / npmDependencies ++= NPM.jose, // NPM.elliptic, // NPM.nodeJose
    // 2Test / scalaJSUseMainModuleInitializer := true, Test / scalaJSUseTestModuleInitializer := false, Test / mainClass := Some("fmgp.crypto.MainTestJS")
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument("--exclude-tags=JsUnsupported"),
  )

lazy val webapp = project
  .in(file("webapp"))
  .settings(name := "fmgp-ipfs-webapp")
  .configure(scalaJSBundlerConfigure)
  .configure(buildInfoConfigure)
  .dependsOn(did.js)
  .settings(
    libraryDependencies ++= Seq(D.laminar.value, D.waypoint.value, D.upickle.value),
    libraryDependencies ++= Seq(D.zio.value, /*D.zioStreams.value,*/ D.zioJson.value),
    Compile / npmDependencies ++= NPM.mermaid ++ NPM.materialDesign ++ NPM.ipfsClient ++
      List("ms" -> "2.1.1"),
    stIgnore ++= List("ms") // https://scalablytyped.org/docs/conversion-options
  )
  .settings(
    webpackBundlingMode := BundlingMode.LibraryAndApplication(), // BundlingMode.Application,
    Compile / scalaJSModuleInitializers += {
      org.scalajs.linker.interface.ModuleInitializer.mainMethod("fmgp.ipfs.webapp.App", "main")
    },
  )
  .settings(noPublishSettings)
