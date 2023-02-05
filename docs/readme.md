# Scala-did

A Scala & Scala.js implementation of DID and DID Comm messaging spec.

DIDComm is a secure messaging protocol for decentralized identities (DIDs).
This library allows you to easily integrate DIDComm messaging into your application.

## Getting Started with Scala-did

To install ScalaDID in a scala project

```scala
 libraryDependencies += "app.fmgp" %% "did" % @VERSION@
 libraryDependencies += "app.fmgp" %% "did.comm" % @VERSION@ //for did comm
 libraryDependencies += "app.fmgp" %% "did.extra" % @VERSION@ //for hash utils
```

In a crossProject for the JSPlatform and JVMPlatform this shod be use instead:

```scala
 libraryDependencies += "app.fmgp" %%% "did" % @VERSION@
 libraryDependencies += "app.fmgp" %%% "did.comm" % @VERSION@ //for did comm
 libraryDependencies += "app.fmgp" %%% "did.extra" % @VERSION@ //for hash utils
```

## Site Map

- This readme [Scala-did](./readme.md)
- [External documentation](./external-documentation.md)
- **Quickstart**
  - [Get Started](./quickstart-get-started.md)
  - [Basic Examples](./quickstart-basic-examples.md)
  - [Setup Environment](./quickstart-setup-environment.md)
    - [scala-cli on docker](./quickstart-setup-environment.md#scala-cli-on-docker)- recommended way just to get the feeling of the library
    - [sbt setup](./quickstart-setup-environment.md#sbt-setup) - for any project.
    - [coursier download](./quickstart-setup-environment.md#coursier-download)
  with some code samples
- **Test**
  - [Test coverage](./test-coverage.md)
  - [Interoperability](./test-interoperability.md)
- [Troubleshooting](./troubleshooting.md)
