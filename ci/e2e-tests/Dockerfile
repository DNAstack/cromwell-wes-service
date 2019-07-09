# Packages the application into a runnable Docker container
FROM openjdk:11-jdk


COPY target/e2e-tests.jar /
COPY target/pom.xml /
COPY target/mvnw /
COPY target/.mvn /.mvn

ENTRYPOINT ["./mvnw", "test"]