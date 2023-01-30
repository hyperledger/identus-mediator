# Setup environment

Setup an environment for quick start

The Library is published to sonatype maven:

## scala-cli on docker

Start an isolated environment to experiment with some code samples (3/5 mins)

`docker run --rm  -it --entrypoint /bin/sh virtuslab/scala-cli`

```bash
scala-cli repl \
  --dependency app.fmgp::did::@VERSION@ \
  --dependency app.fmgp::did-imp::@VERSION@ \
  --dependency app.fmgp::did-resolver-peer::@VERSION@ \
  --repo https://oss.sonatype.org/content/repositories/snapshots

# For snapshots use
# --repo https://oss.sonatype.org/content/repositories/snapshots
# For releases use
# --repo https://oss.sonatype.org/content/repositories/releases
```

## SBT setup

To install the library on `sbt`, you can use the following lines to your `build.sbt`:

```sbt
 libraryDependencies += "app.fmgp" %% "did" % @VERSION@
 libraryDependencies += "app.fmgp" %% "did-imp" % @VERSION@ //for did comm
 libraryDependencies += "app.fmgp" %% "did-resolver-peer" % @VERSION@ //for hash utils
```

In a crossProject for the JSPlatform and JVMPlatform this shoud use this instead:

```sbt
 libraryDependencies += "app.fmgp" %%% "did" % @VERSION@
 libraryDependencies += "app.fmgp" %%% "did-imp" % @VERSION@ //for did comm
 libraryDependencies += "app.fmgp" %%% "did-resolver-peer" % @VERSION@ //for hash utils
```

## Coursier Download

```bash 
coursier fetch app.fmgp:scala-did:@VERSION@ -r sonatype:snapshots
# -r https://oss.sonatype.org/content/repositories/snapshots

coursier fetch app.fmgp:scala-did:@VERSION@ -r sonatype:public
# -r https://oss.sonatype.org/content/repositories/releases
```
