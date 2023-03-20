resolvers ++= Resolver.sonatypeOssRepos("public")
resolvers ++= Resolver.sonatypeOssRepos("snapshots")

inThisBuild(
  Seq(
    scalaVersion := "3.2.2", // Also update docs/publishWebsite.sh and any ref to scala-3.2.2
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
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
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
    // updateOptions := updateOptions.value.withLatestSnapshots(false),
    versionScheme := Some("early-semver"), // https://www.scala-sbt.org/1.x/docs/Publishing.html#Version+scheme
  )
)
lazy val notYetPublishedConfigure: Project => Project = _.settings(
  publish / skip := true
)

// ### publish Github ###
lazy val publishConfigure: Project => Project = _.settings(
  // For publish to Github
  // sonatypeSnapshotResolver := MavenRepository("sonatype-snapshots", s"https://${sonatypeCredentialHost.value}")
)
// inThisBuild(
//   Seq(
//     sonatypeCredentialHost := "maven.pkg.github.com/FabioPinheiro/scala-did",
//     sonatypeRepository := "https://maven.pkg.github.com",
//     fork := true,
//     Test / fork := false, // If true we get a Error: `test / test` tasks in a Scala.js project require `test / fork := false`.
//     run / connectInput := true,
//   ) ++ scala.util.Properties
//     .envOrNone("PACKAGES_GITHUB_TOKEN")
//     .map(passwd =>
//       credentials += Credentials(
//         "GitHub Package Registry",
//         "maven.pkg.github.com",
//         "FabioPinheiro",
//         passwd
//       )
//     )
// )

/** run with 'docs/mdoc' */
lazy val docs = project // new documentation project
  .in(file("docs-build")) // important: it must not be docs/
  .settings(skip / publish := true)
  .settings(
    cleanFiles += baseDirectory.value / "docs-build",
    //   mdocJS := Some(webapp),
    //   // https://scalameta.org/mdoc/docs/js.html#using-scalajs-bundler
    //   mdocJSLibraries := ((webapp / Compile / fullOptJS) / webpack).value,
    mdoc := {
      //     val log = streams.value.log
      (mdoc).evaluated
      //     scala.sys.process.Process("pwd") ! log
      //     scala.sys.process.Process(
      //       "md2html" :: "docs-build/target/mdoc/readme.md" :: Nil
      //     ) #> file("docs-build/target/mdoc/readme.html") ! log
    },
  )
  .settings(mdocVariables := Map("VERSION" -> version.value))
  .dependsOn(did.jvm) // , webapp) // jsdocs)
  .enablePlugins(MdocPlugin) // , DocusaurusPlugin)

/** Versions */
lazy val V = new {
  val scalajsJavaSecureRandom = "1.0.0"

  // FIXME another bug in the test framework https://github.com/scalameta/munit/issues/554
  val munit = "1.0.0-M7" // "0.7.29"

  // https://mvnrepository.com/artifact/org.scala-js/scalajs-dom
  val scalajsDom = "2.3.0"
  // val scalajsLogging = "1.1.2-SNAPSHOT" //"1.1.2"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.10"
  val zioJson = "0.4.2"
  val zioMunitTest = "0.1.1"
  val zioHttp = "0.0.4"
  val zioPrelude = "1.0.0-RC16"

  // https://mvnrepository.com/artifact/io.github.cquiroz/scala-java-time
  val scalaJavaTime = "2.3.0"

  val logbackClassic = "1.2.10"
  val scalaLogging = "3.9.4"

  val laika = "0.19.0"

  val laminar = "0.14.5"
  val waypoint = "0.5.0"
  val upickle = "2.0.0"
  // https://www.npmjs.com/package/material-components-web
  val materialComponents = "12.0.0"
}

/** Dependencies */
lazy val D = new {

  /** The [[java.security.SecureRandom]] is used by the [[java.util.UUID.randomUUID()]] method in [[MsgId]].
    *
    * See more https://github.com/scala-js/scala-js-java-securerandom
    */
  val scalajsJavaSecureRandom = Def.setting(
    ("org.scala-js" %%% "scalajs-java-securerandom" % V.scalajsJavaSecureRandom)
      .cross(CrossVersion.for3Use2_13)
  )

  val dom = Def.setting("org.scala-js" %%% "scalajs-dom" % V.scalajsDom)

  val zio = Def.setting("dev.zio" %%% "zio" % V.zio)
  val zioStreams = Def.setting("dev.zio" %%% "zio-streams" % V.zio)
  val zioJson = Def.setting("dev.zio" %%% "zio-json" % V.zioJson)
  val ziohttp = Def.setting("dev.zio" %% "zio-http" % V.zioHttp)
  val zioPrelude = Def.setting("dev.zio" %%% "zio-prelude" % V.zioPrelude)
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

  val laika = Def.setting("org.planet42" %%% "laika-core" % V.laika) // JVM & JS

  // For WEBAPP
  val laminar = Def.setting("com.raquo" %%% "laminar" % V.laminar)
  val waypoint = Def.setting("com.raquo" %%% "waypoint" % V.waypoint)
  val upickle = Def.setting("com.lihaoyi" %%% "upickle" % V.upickle)
}

/** NPM Dependencies */
lazy val NPM = new {
  // https://www.npmjs.com/package/@types/d3
  // val d3NpmDependencies = Seq("d3", "@types/d3").map(_ -> "7.1.0")

  val mermaid = Seq("mermaid" -> "9.3.0") // "@types/mermaid" -> "9.2.0"

  val materialDesign = Seq("material-components-web" -> V.materialComponents)

  val ipfsClient = Seq("multiformats" -> "9.6.4")

  // val nodeJose = Seq("node-jose" -> "2.1.1", "@types/node-jose" -> "1.1.10")
  // val elliptic = Seq("elliptic" -> "6.5.4", "@types/elliptic" -> "6.4.14")
  val jose = Seq("jose" -> "4.8.3")
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

/** https://docs.scala-lang.org/scala3/guides/scaladoc/settings.html */
lazy val docConfigure: Project => Project =
  _.settings(
    autoAPIMappings := true,
    Compile / doc / target := {
      val path =
        baseDirectory.value.toPath.getParent.getParent /
          "docs-build" / "target" / "api" /
          name.value / baseDirectory.value.getName
      // println(path)
      path.toFile
    },
    apiURL := Some(url(s"https://did.fmgp.app/apis/${name.value}/${baseDirectory.value.getName}")),
  )

addCommandAlias(
  "testJVM",
  ";didJVM/test; didExtraJVM/test; didImpJVM/test; " +
    "didResolverPeerJVM/test; didResolverWebJVM/test; didUniresolverJVM/test; " +
    "multiformatsJVM/test"
)
addCommandAlias(
  "testJS",
  ";didJS/test;  didExtraJS/test;  didImpJS/test;  " +
    "didResolverPeerJS/test;  didResolverWebJS/test;  didUniresolverJS/test;  " +
    "multiformatsJS/test"
)
addCommandAlias("testAll", ";testJVM;testJS")
addCommandAlias("compileAll", "docs/mdoc;compile")
addCommandAlias("cleanAll", "clean;docs/clean")

lazy val root = project
  .in(file("."))
  .settings(publish / skip := true)
  .aggregate(did.js, did.jvm) // publish
  .aggregate(didExtra.js, didExtra.jvm) // publish
  .aggregate(didImp.js, didImp.jvm) // publish
  .aggregate(multiformats.js, multiformats.jvm) // publish
  .aggregate(didResolverPeer.js, didResolverPeer.jvm) // publish
  .aggregate(didResolverWeb.js, didResolverWeb.jvm) // publish
  .aggregate(didUniresolver.js, didUniresolver.jvm) // NOT publish
  .aggregate(didExample.js, didExample.jvm)
  .aggregate(demo.jvm, demo.js)
  .aggregate(webapp)

lazy val did = crossProject(JSPlatform, JVMPlatform)
  .in(file("did"))
  .configure(publishConfigure)
  .settings((setupTestConfig): _*)
  .settings(Test / scalacOptions -= "-Ysafe-init") // TODO REMOVE Cannot prove the method argument is hot.
  .settings(
    name := "did",
    libraryDependencies += D.zioJson.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jsSettings(libraryDependencies += D.scalajsJavaSecureRandom.value.cross(CrossVersion.for3Use2_13))
  .configure(docConfigure)

lazy val didExtra = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-extra"))
  .configure(notYetPublishedConfigure)
  .settings(Test / scalacOptions -= "-Ysafe-init") // TODO REMOVE Cannot prove the method argument is hot.
  .settings(
    name := "did-extra",
    libraryDependencies += D.zioPrelude.value, // just for the hash (is this over power?)
    libraryDependencies += D.zioMunitTest.value,
  )
  .dependsOn(did % "compile;test->test")
  .configure(docConfigure)

lazy val didImp = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-imp"))
  .configure(publishConfigure)
  .settings((setupTestConfig): _*)
  .settings(Test / scalacOptions -= "-Ysafe-init") // TODO REMOVE Cannot prove the method argument is hot.
  .settings(name := "did-imp")
  .settings(libraryDependencies += D.zioMunitTest.value)
  .jvmSettings( // Add JVM-specific settings here
    libraryDependencies += "org.bouncycastle" % "bcprov-jdk18on" % "1.72", // https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
    libraryDependencies += "org.bouncycastle" % "bcpkix-jdk18on" % "1.72", // https://mvnrepository.com/artifact/org.bouncycastle/bcpkix-jdk18on
    libraryDependencies += "com.nimbusds" % "nimbus-jose-jwt" % "9.30.2", // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt/9.23

    // BUT have vulnerabilities in the dependencies: CVE-2022-25647
    libraryDependencies += "com.google.crypto.tink" % "tink" % "1.7.0", // https://mvnrepository.com/artifact/com.google.crypto.tink/tink/1.6.1
    // To fix vulnerabilitie https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-25647
    libraryDependencies += "com.google.code.gson" % "gson" % "2.10.1",
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.22.0",
  )
  .jsConfigure(scalaJSBundlerConfigure)
  .jsSettings( // Add JS-specific settings here
    stShortModuleNames := true,
    Compile / npmDependencies ++= NPM.jose, // NPM.elliptic, // NPM.nodeJose
    // Test / scalaJSUseMainModuleInitializer := true, Test / scalaJSUseTestModuleInitializer := false, Test / mainClass := Some("fmgp.crypto.MainTestJS")
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument("--exclude-tags=JsUnsupported"),
  )
  .dependsOn(did % "compile;test->test")
  .configure(docConfigure)

/** This is a copy of https://github.com/fluency03/scala-multibase to support crossProject
  *
  * "com.github.fluency03" % "scala-multibase_2.12" % "0.0.1"
  */
lazy val multiformats =
  crossProject(JSPlatform, JVMPlatform)
    .in(file("multiformats"))
    .configure(publishConfigure)
    .settings(Test / scalacOptions -= "-Ysafe-init") // TODO REMOVE Cannot prove the method argument is hot.
    .settings(
      name := "multiformats",
      libraryDependencies += D.munit.value,
      libraryDependencies += D.zioMunitTest.value,
    )
    .configure(docConfigure)

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
  .jsConfigure(scalaJSBundlerConfigure)
  .dependsOn(did, multiformats)
  .dependsOn(didImp % "test->test") // To generate keys for tests
  .configure(docConfigure)

//https://w3c-ccg.github.io/did-method-web/
lazy val didResolverWeb = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-resolver-web"))
  .configure(notYetPublishedConfigure)
  .settings(
    name := "did-web",
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jvmSettings(libraryDependencies += D.ziohttp.value)
  .dependsOn(did)
  .configure(docConfigure)

//https://dev.uniresolver.io/
lazy val didUniresolver = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-resolver-uniresolver"))
  .settings(publish / skip := true)
  .configure(notYetPublishedConfigure)
  .settings(
    name := "did-uniresolver",
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jvmSettings(libraryDependencies += D.ziohttp.value)
  // .enablePlugins(ScalaJSBundlerPlugin).jsSettings(Test / npmDependencies += "node-fetch" -> "3.3.0")
  .jsSettings( // TODO https://scalacenter.github.io/scalajs-bundler/reference.html#jsdom
    libraryDependencies += D.dom.value,
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    Test / requireJsDomEnv := true,
  )
  .dependsOn(did)
  .configure(docConfigure)

lazy val webapp = project
  .in(file("webapp"))
  .settings(publish / skip := true)
  .settings(name := "fmgp-webapp")
  .configure(scalaJSBundlerConfigure)
  .configure(buildInfoConfigure)
  .dependsOn(did.js, didExample.js)
  .settings(
    libraryDependencies ++= Seq(D.laminar.value, D.waypoint.value, D.upickle.value),
    libraryDependencies ++= Seq(D.zio.value, D.zioJson.value),
    Compile / npmDependencies ++= NPM.mermaid ++ NPM.materialDesign ++ NPM.ipfsClient,
    // ++ List("ms" -> "2.1.1"),
    // stIgnore ++= List("ms") // https://scalablytyped.org/docs/conversion-options
  )
  .settings( // for doc
    libraryDependencies += D.laika.value,
    Compile / sourceGenerators += makeDocSources.taskValue,
  )
  .settings(
    stShortModuleNames := true,
    webpackBundlingMode := BundlingMode.LibraryAndApplication(), // BundlingMode.Application,
    Compile / scalaJSModuleInitializers += {
      org.scalajs.linker.interface.ModuleInitializer.mainMethod("fmgp.webapp.App", "main")
    },
  )

lazy val didExample = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-example"))
  .settings(publish / skip := true)
  .dependsOn(did, didImp, didResolverPeer, didResolverWeb, didUniresolver)

lazy val demo = crossProject(JSPlatform, JVMPlatform)
  .in(file("demo"))
  .settings(publish / skip := true)
  .settings(
    name := "did-demo",
    libraryDependencies += D.zioStreams.value,
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
    libraryDependencies += D.laika.value,
  )
  .jvmSettings(
    reStart / mainClass := Some("fmgp.did.demo.AppServer"),
    assembly / mainClass := Some("fmgp.did.demo.AppServer"),
    assembly / assemblyJarName := "scala-did-demo-server.jar",
    libraryDependencies += D.ziohttp.value,

    // WebScalaJSBundlerPlugin
    scalaJSProjects := Seq(webapp),
    /** scalaJSPipeline task runs scalaJSDev when isDevMode is true, runs scalaJSProd otherwise. scalaJSProd task runs
      * all tasks for production, including Scala.js fullOptJS task and source maps scalaJSDev task runs all tasks for
      * development, including Scala.js fastOptJS task and source maps.
      */
    Assets / pipelineStages := Seq(scalaJSPipeline),
    // pipelineStages ++= Seq(digest, gzip), //Compression - If you serve your Scala.js application from a web server, you should additionally gzip the resulting .js files.
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "extra-resources",
    // Compile / unmanagedResourceDirectories += (baseDirectory.value.toPath.getParent.getParent / "docs-build" / "target" / "api").toFile,
    Compile / unmanagedResourceDirectories += (baseDirectory.value.toPath.getParent.getParent / "docs-build" / "target" / "mdoc").toFile,
    Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
    // Frontend dependency configuration
    Assets / WebKeys.packagePrefix := "public/",
    Runtime / managedClasspath += (Assets / packageBin).value,
  )
  .dependsOn(did, didImp, didResolverPeer, didResolverWeb, didUniresolver, didExample)
  .enablePlugins(WebScalaJSBundlerPlugin)

ThisBuild / assemblyMergeStrategy := {
  case "META-INF/versions/9/module-info.class" => MergeStrategy.first
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
//   case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
//   case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
//   case "application.conf"                            => MergeStrategy.concat
//   case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

/** Copy the Documentation and Generate an Scala object to Store */
def makeDocSources = Def.task {
  val baseDir = baseDirectory.value.toPath.getParent / "docs-build" / "target" / "mdoc"
  val resourceFile = (baseDir / "readme.md").toFile
  val sourceDir = (Compile / sourceManaged).value
  val sourceFile = sourceDir / "DocSource.scala"
  if (!sourceFile.exists() || sourceFile.lastModified() < resourceFile.lastModified()) {
    val contentREAMDE = IO.read(resourceFile).replaceAllLiterally("$", "$$").replaceAllLiterally("\"\"\"", "\"\"$\"")
    val scalaCode = s"""
      |package fmgp.did
      |object DocSource {
      |  final val readme = raw\"\"\"$contentREAMDE\"\"\"
      |}""".stripMargin
    IO.write(sourceFile, scalaCode)
  }
  Seq(sourceFile)
}
