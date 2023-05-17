# Publish / Release

```
did_3
did_sjs1_3
did-imp_3
did-imp_sjs1_3
did-method-peer_3
did-method-peer_sjs1_3
did-method-web_3
did-method-web_sjs1_3
multiformats_3
multiformats_sjs1_3
```

## Publish Local

```sbt
publishLocal
```

## Publish to Sonatype

The library is publish to https://oss.sonatype.org/

### Config 

```zsh
gpg -k fabiomgpinheiro+Scala-Steward@gmail.com
gpg --armor --export $LONG_ID # Get the LONG_ID from the 
# gpg --armor --export 212722AA6D9EC1A86799D54924607459115D94B0

gpg --keyserver hkp://keyserver.ubuntu.com --send-key $LONG_ID && \
 gpg --keyserver hkp://pgp.mit.edu --send-key $LONG_ID && \
 gpg --keyserver hkp://pool.sks-keyservers.net --send-key $LONG_ID

 gpg --armor --export-secret-keys $LONG_ID | base64 | sed -z 's;\n;;g' | xclip -selection clipboard -i # for the PGP_SECRET
 gpg --armor --export-secret-keys 212722AA6D9EC1A86799D54924607459115D94B0 | base64 | sed -z 's;\n;;g' | xclip -selection clipboard -i # for the PGP_SECRET
```

### Config CI

**GitHub action** after pushing a tag (started with 'v') the action `.github/workflows/ci.yml` will try to make a release (Publish to the libraries to Sonatype).
For `SNAPSHOT` call the action manual with the flag `make_snapshot_release` set to true.

Job's ENV notes:
- `SONATYPE_PASSWORD` - The password you use to log into https://s01.oss.sonatype.org/
- `SONATYPE_USERNAME` - The username you use to log into https://s01.oss.sonatype.org
- `PGP_SECRET` -  The base64 encoded secret of your private key

### Run ci-release Local


`NODE_OPTIONS=--openssl-legacy-provider SONATYPE_USERNAME=??? SONATYPE_PASSWORD=??? PGP_SECRET=??? sbt`

### Check that is release

`coursier fetch app.fmgp:scala-did:0.1.0 -r sonatype:public`
`coursier fetch app.fmgp:scala-did:0.1.0-SNAPSHOT -r sonatype:snapshots`

## Publish Github (deprecated)

Publish to Github is disabled (commented) on build.sbt.
But the configuration should still work.

The reason to be disabled is due to the limitations of GitHub.

Even if the project is open source you need a GitHub account to download the packages.

The Library was also published to github `https://maven.pkg.github.com/FabioPinheiro/scala-did`.

