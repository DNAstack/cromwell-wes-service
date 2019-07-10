FROM openjdk:11-slim

ADD target/app.jar /app.jar

EXPOSE 8080

ENTRYPOINT java ${JAVA_OPTS} -jar app.jar