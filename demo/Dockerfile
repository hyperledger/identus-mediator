FROM azul/zulu-openjdk-alpine:11

COPY ./jvm/target/scala-3.2.2/scala-did-demo-server.jar /app/app.jar
WORKDIR /app

EXPOSE 8080

ENTRYPOINT [ "java", "-Dport=8080", "-XX:+UseContainerSupport", "-jar", "app.jar" ]
#-Xmx100m
#-Xss256k
#-XX:MaxMetaspaceSize=N
#-XX:+UseContainerSupport
