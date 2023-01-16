# Examples

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


## Import all

Here are the usfull imports for most of the cases:

```scala mdoc
import zio.json._
import fmgp.did._
import fmgp.did.comm._
```


## Parce a DID Document

```scala mdoc:silent
val documentString = """{
  "@context": "https://www.w3.org/ns/did/v1",
  "id": "did:example:123456789abcdefghi",
  "keyAgreement": [
    "did:example:123456789abcdefghi#keys-1",
    {
      "id": "did:example:123#zC9ByQ8aJs8vrNXyDhPHHNNMSHPcaSgNpjjsBYpMMjsTdS",
      "type": "X25519KeyAgreementKey2019",
      "controller": "did:example:123",
      "publicKeyMultibase": "z9hFgmPVfmBZwRvFEyniQDBkz9LmV7gDEqytWyGZLmDXE"
    }
  ]
}"""
```
```scala mdoc
documentString.fromJson[DIDDocument]
```

**NOTE** the return type is of type `Either[String, DIDDocument]`
<br> Since the `documentString` is a valid json and which is also a valid DID Document.
The value is `Right` of that class that implemente trait `DIDDocument`.
<br> If the `documentString` was a invalid json or did not represented a DID Document.
The return value would have been Left, containing the information why failed to parse.

```scala mdoc
"not a json".fromJson[DIDDocument]
```

```scala mdoc
"""{"some_json": "but_not_a_valid_document"}"""
  .fromJson[DIDDocument]
```

Another **Important Point** is that there is no failed here everywhere on this library! We work with values.
___(Any error/exception will be considered a bug and reports are appreciated)___

This this allowed us to this allowed us to build programs build (ZIO) programs. That can are executed at any point in time.