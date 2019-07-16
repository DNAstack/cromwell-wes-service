# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

# This stage depends on a JDK image and uses Maven Wrapper to pull in dependencies and build the project
# from source.
FROM openjdk:11-jdk-slim AS build
ARG APP_NAME
ARG APP_VERSION

RUN apt-get update
RUN apt-get -y install curl libatomic1

WORKDIR /build

# Before bringing in project sources, resolve maven dependencies.
# This allows Docker to reuse the cached image with all dependencies resolved.
ADD target/pom.xml ./
ADD target/.mvn ./.mvn
ADD target/mvnw ./
RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
RUN ./mvnw -B versions:set -DnewVersion=${APP_VERSION}

# Now add sources, which will bust the cache.
ADD target/src ./src
RUN ./mvnw -B -o package -DskipTests=true
RUN ls -lah /build/target/${APP_NAME}*.jar

##
## Stage 2: Package the runnable test image
##

# This stage uses a pre-packaged Selenium Docker image.
# It is assembling the runnable test container and packaging all its Maven dependencies
# produced by the previous stage.
FROM openjdk:11-slim
ARG APP_NAME
ARG APP_VERSION

ADD target/e2e-exec-pom.xml /pom.xml
ADD target/.mvn /.mvn
ADD target/mvnw /

# include all the files assembled for us by the build-docker-e2e-image script
USER root

COPY --from=0 /build/target/${APP_NAME}*.jar /e2e-tests.jar
RUN mkdir /target
RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

ENTRYPOINT ["./mvnw", "test"]