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
sbt demoJVM/assembly
java -jar jvm/target/scala-3.2.2-RC1/scala-did-demo-server.jar
docker build --tag scala_did_demo .
docker run --rm -p 8080:8080 --memory="100m" --cpus="1.0" scala_did_demo
```

```
jar tf /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.2.2-RC1/scala-did-demo-server.jar | less
java -jar /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.2.2-RC1/scala-did-demo-server.jar
```

## FLY.IO

```shell
<!-- flyctl launch -->
flyctl deploy

flyctl open /demo

flyctl status
```