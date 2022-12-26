# DEMO

## run

```sbt
~ demoJVM/reStart
```

### test

```shell
curl localhost:8080/demo
```

## docker
```shell
NODE_OPTIONS=--openssl-legacy-provider sbt demoJVM/assembly
java -jar jvm/target/scala-3.2.2-RC2/scala-did-demo-server.jar
docker build --tag scala_did_demo .
docker run --rm -p 8080:8080 --memory="100m" --cpus="1.0" scala_did_demo
```

```
jar tf /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.2.2-RC2/scala-did-demo-server.jar | less
java -jar /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.2.2-RC2/scala-did-demo-server.jar
```

## FLY.IO

- `flyctl auth login`
- `flyctl open /demo`
- `flyctl status -a scala-did-demo`
- `flyctl image show -a scala-did-demo`

**deploy with flyctl**

```shell
NODE_OPTIONS=--openssl-legacy-provider sbt demoJVM/assembly
flyctl deploy
```

**[WIP] deploy by pushing docker image**

```shell
NODE_OPTIONS=--openssl-legacy-provider sbt demoJVM/assembly
docker build --tag scala_did_demo ./demo/
docker tag scala_did_demo registry.fly.io/scala-did-demo
# flyctl auth docker
docker push registry.fly.io/scala-did-demo # +- 52MB

flyctl image update -a scala-did-demo
# FIXME: Error image is not eligible for automated image updates
```