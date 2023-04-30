# Scala-did

A Scala & Scala.js implementation of DID and DID Comm messaging spec.

DIDComm is a secure messaging protocol for decentralized identities (DIDs).
This library allows you to easily integrate DIDComm messaging into your application.

## Getting Started with Scala-did

To install ScalaDID in a scala project

```scala
 libraryDependencies += "app.fmgp" %% "did" % @VERSION@ // for DID and DID Comm
 libraryDependencies += "app.fmgp" %% "did-imp" % @VERSION@ // for crypto implementation
//  libraryDependencies += "app.fmgp" %% "did-extra" % @VERSION@ //for hash utils
 libraryDependencies += "app.fmgp" %% "did-peer" % @VERSION@ // for resolver of the did method `peer`
 libraryDependencies += "app.fmgp" %% "did-web" % @VERSION@ // for resolver the did method `web`
 libraryDependencies += "app.fmgp" %% "did-uniresolver" % @VERSION@ // for calling the resolver uniresolver
```

In a crossProject for the JSPlatform and JVMPlatform this should be `%%%` instead of `%%`


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
- [Limitations](./limitations.md)
- [Troubleshooting](./troubleshooting.md)
