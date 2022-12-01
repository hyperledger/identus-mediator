inThisBuild(
  Seq(
    scalaVersion := "3.2.2-RC1", // Also update docs/publishWebsite.sh and any ref to scala-3.2.2-RC1
  )
)
// publish config
inThisBuild(
  Seq(
    Test / publishArtifact := false,
    // pomIncludeRepository := (_ => false),
    organization := "app.fmgp",
    homepage := Some(url("https://github.com/FabioPinheiro/scala-did")),
    licenses := Seq(
      "Apache-2.0" ->
        url("http://www.apache.org/licenses/LICENSE-2.0")
        // url ("https://github.com/FabioPinheiro/scala-did" + "/blob/master/LICENSE")
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/FabioPinheiro/scala-did"),
        "scm:git:git@github.com:FabioPinheiro/scala-did.git"
      )
    ),
    developers := List(
      Developer("FabioPinheiro", "Fabio Pinheiro", "fabiomgpinheiro@gmail.com", url("http://fmgp.app"))
    ),
    updateOptions := updateOptions.value.withLatestSnapshots(false),

    // ### publish Github ###
    sonatypeCredentialHost := "maven.pkg.github.com/FabioPinheiro/scala-did",
    sonatypeRepository := "https://maven.pkg.github.com",
    versionScheme := Some("early-semver"),
    fork := true,
    Test / fork := false, // If true we get a Error: `test / test` tasks in a Scala.js project require `test / fork := false`.
    run / connectInput := true,
  ) ++ scala.util.Properties
    .envOrNone("PACKAGES_GITHUB_TOKEN")
    .map(passwd =>
      credentials += Credentials(
        "GitHub Package Registry",
        "maven.pkg.github.com",
        "FabioPinheiro",
        passwd
      )
    )
)

lazy val docs = project // new documentation project
  .in(file("docs-build")) // important: it must not be docs/
  .settings(skip / publish := true)
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
  val munit = "1.0.0-M7" // "0.7.29"

  // https://mvnrepository.com/artifact/org.scala-js/scalajs-dom
  val scalajsDom = "2.3.0"
  // val scalajsLogging = "1.1.2-SNAPSHOT" //"1.1.2"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.4"
  val zioJson = "0.3.0"
  val zioMunitTest = "0.1.1"
  val zioHttp = "0.0.3"

  // https://mvnrepository.com/artifact/io.github.cquiroz/scala-java-time
  val scalaJavaTime = "2.3.0"

  val logbackClassic = "1.2.10"
  val scalaLogging = "3.9.4"

  val laminar = "0.14.5"
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
  val ziohttp = Def.setting("dev.zio" %% "zio-http" % V.zioHttp)
  // val zioTest = Def.setting("dev.zio" %%% "zio-test" % V.zio % Test)
  // val zioTestSBT = Def.setting("dev.zio" %%% "zio-test-sbt" % V.zio % Test)
  // val zioTestMagnolia = Def.setting("dev.zio" %%% "zio-test-magnolia" % V.zio % Test)
  val zioMunitTest = Def.setting("com.github.poslegm" %%% "munit-zio" % V.zioMunitTest % Test)

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

  // val mermaid = Seq("mermaid" -> "8.14.0", "@types/mermaid" -> "8.2.8")
  val mermaid = Seq("mermaid" -> "9.1.6", "@types/mermaid" -> "8.2.9")

  val materialDesign = Seq("material-components-web" -> V.materialComponents)

  val ipfsClient = Seq("multiformats" -> "9.6.4")

  // val nodeJose = Seq("node-jose" -> "2.1.1", "@types/node-jose" -> "1.1.10")
  // val elliptic = Seq("elliptic" -> "6.5.4", "@types/elliptic" -> "6.4.14")
  val jose = Seq("jose" -> "4.8.3")
}

lazy val settingsFlags: Seq[sbt.Def.SettingsDefinition] = Seq(
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8", // source files are in UTF-8
    "-deprecation", // warn about use of deprecated APIs
    "-unchecked", // warn about unchecked type parameters
    "-feature", // warn about misused language features
    "-Xfatal-warnings",
    // TODO "-Yexplicit-nulls",
    // TODO  "-Ysafe-init",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-Xprint-diff-del", // "-Xprint-diff",
    "-Xprint-inline",
  )
)

lazy val setupTestConfig: Seq[sbt.Def.SettingsDefinition] = Seq(
  libraryDependencies += D.munit.value,
)

lazy val commonSettings: Seq[sbt.Def.SettingsDefinition] = settingsFlags ++ Seq(
  Compile / doc / sources := Nil,
)

lazy val scalaJSBundlerConfigure: Project => Project =
  _.settings(commonSettings: _*)
    .enablePlugins(ScalaJSPlugin)
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings((setupTestConfig): _*)
    .settings(
      scalaJSLinkerConfig ~= {
        _.withSourceMap(false) // disabled because it somehow triggers many warnings
          .withModuleKind(ModuleKind.CommonJSModule)
          .withJSHeader(
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

lazy val publishConfigure: Project => Project = _.settings(
  sonatypeSnapshotResolver := MavenRepository("sonatype-snapshots", s"https://${sonatypeCredentialHost.value}")
)

addCommandAlias(
  "testJVM",
  ";didJVM/test; didImpJVM/test; didResolverPeerJVM/test; didResolverWebJVM/test; multiformatsJVM/test"
)
addCommandAlias(
  "testJS",
  ";didJS/test;  didImpJS/test;  didResolverPeerJS/test;  didResolverWebJS/test;  multiformatsJS/test"
)
addCommandAlias("testAll", ";testJVM;testJS")

lazy val root = project
  .in(file("."))
  .settings(publish / skip := true)
  .aggregate(did.js, did.jvm)
  .aggregate(didImp.js, didImp.jvm)
  .aggregate(didResolverPeer.js, didResolverPeer.jvm)
  .aggregate(didResolverWeb.js, didResolverWeb.jvm)
  .aggregate(webapp)
  .settings(commonSettings: _*)

lazy val did = crossProject(JSPlatform, JVMPlatform)
  .in(file("did"))
  .configure(publishConfigure)
  .settings((setupTestConfig): _*)
  .settings(
    name := "did",
    libraryDependencies += D.zioJson.value,
    // libraryDependencies += D.zioTest.value,
    // libraryDependencies += D.zioTestSBT.value,
    libraryDependencies += D.zioMunitTest.value,
    // testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val didImp = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-imp"))
  .configure(publishConfigure)
  .settings((setupTestConfig): _*)
  .settings(name := "did-imp")
  .settings(libraryDependencies += D.zioMunitTest.value)
  .dependsOn(did % "compile;test->test")
  .jvmSettings( // Add JVM-specific settings here
    libraryDependencies += "org.bouncycastle" % "bcprov-jdk18on" % "1.72", // https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
    libraryDependencies += "org.bouncycastle" % "bcpkix-jdk18on" % "1.72", // https://mvnrepository.com/artifact/org.bouncycastle/bcpkix-jdk18on
    libraryDependencies += "com.nimbusds" % "nimbus-jose-jwt" % "9.25.6", // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt/9.23

    // BUT have vulnerabilities in the dependencies: CVE-2022-25647
    libraryDependencies += "com.google.crypto.tink" % "tink" % "1.7.0", // https://mvnrepository.com/artifact/com.google.crypto.tink/tink/1.6.1
    // To fix vulnerabilitie https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-25647
    libraryDependencies += "com.google.code.gson" % "gson" % "2.10",
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.21.10",
  )
  // .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
  .jsConfigure(scalaJSBundlerConfigure)
  .jsSettings( // Add JS-specific settings here
    stShortModuleNames := true,
    Compile / npmDependencies ++= NPM.jose, // NPM.elliptic, // NPM.nodeJose
    // 2Test / scalaJSUseMainModuleInitializer := true, Test / scalaJSUseTestModuleInitializer := false, Test / mainClass := Some("fmgp.crypto.MainTestJS")
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument("--exclude-tags=JsUnsupported"),
  )

/** This is a copy of https://github.com/fluency03/scala-multibase to support crossProject
  *
  * "com.github.fluency03" % "scala-multibase_2.12" % "0.0.1"
  */
lazy val multiformats =
  crossProject(JSPlatform, JVMPlatform)
    .in(file("multiformats"))
    .configure(publishConfigure)
    .settings(
      name := "multiformats",
      libraryDependencies += D.munit.value,
      libraryDependencies += D.zioMunitTest.value,
    )

lazy val didResolverPeer = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-resolver-peer"))
  .configure(publishConfigure)
  .settings(
    name := "did-peer",
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jvmSettings( // See dependencyTree ->  didResolverPeerJVM/Test/dependencyTree
    libraryDependencies += "org.didcommx" % "didcomm" % "0.3.2" % Test,
    libraryDependencies += "org.didcommx" % "peerdid" % "0.3.0" % Test,
    libraryDependencies += "org.bouncycastle" % "bcprov-jdk18on" % "1.72" % Test,
    libraryDependencies += "org.bouncycastle" % "bcpkix-jdk18on" % "1.72" % Test,
    libraryDependencies += "com.nimbusds" % "nimbus-jose-jwt" % "9.16-preview.1" % Test,
  )
  .dependsOn(did, multiformats)

//https://w3c-ccg.github.io/did-method-web/
lazy val didResolverWeb = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-resolver-web"))
  .configure(publishConfigure)
  .settings(
    name := "did-web",
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jvmSettings(
    libraryDependencies += D.ziohttp.value,
  )
  .dependsOn(did)

lazy val demo = crossProject(JSPlatform, JVMPlatform)
  .in(file("demo"))
  .configure(publishConfigure)
  .settings(
    name := "did-demo",
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jvmSettings(
    libraryDependencies += D.ziohttp.value,
  )
  .dependsOn(did, didImp, didResolverPeer, didResolverWeb)

lazy val webapp = project
  .in(file("webapp"))
  .settings(publish / skip := true)
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
    stShortModuleNames := true,
    webpackBundlingMode := BundlingMode.LibraryAndApplication(), // BundlingMode.Application,
    Compile / scalaJSModuleInitializers += {
      org.scalajs.linker.interface.ModuleInitializer.mainMethod("fmgp.ipfs.webapp.App", "main")
    },
  )
