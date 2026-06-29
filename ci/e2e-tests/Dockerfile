# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

# This stage depends on a JDK image and uses Maven Wrapper to pull in dependencies and build the project
# from source.
FROM amazoncorretto:25.0.3-al2023@sha256:e15df19a0bc75d68f177d5006a9b1145b5a491c422679e6d729b91a635ab9d9e AS build

ARG APP_NAME
ARG APP_VERSION

WORKDIR /build

# Before bringing in project sources, resolve maven dependencies.
# This allows Docker to reuse the cached image with all dependencies resolved.
ADD target/pom.xml ./
ADD target/.mvn ./.mvn
ADD target/mvnw ./
# Downloads npm and node in separate layer from running "npm install"
RUN ./mvnw -P -dnastack -B initialize
# Allows us to pass settings.xml configured on local machine or CI server to access private Nexus repo
ADD target/.m2 /root/.m2
# Resolves all maven dependencies and frontend dependencies (via "npm install")
RUN ./mvnw -P -dnastack -B -Presolve-dependencies initialize
RUN ./mvnw -P -dnastack -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
RUN ./mvnw -P -dnastack -B versions:set -DnewVersion=${APP_VERSION}

# Delete settings*.xml files that contain GitHub credentials
RUN rm /root/.m2/settings*.xml

# Now add sources, which will bust the cache.
ADD target/src ./src

# If this step fails with an error like
#     Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:2.22.1:test
#     (default-test) on project xxx: Unable to generate classpath:
#     org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException: Missing:
# then update the version number of surefire-junit4 in pom.xml under the
# dynamicDependencies section of go-offline-maven-plugin configuration.
RUN ./mvnw -P -dnastack -B -o process-test-classes


##
## Stage 2: Package the runnable image
##

# This stage uses a JRE rather than JDK because it doesn't have to build anything; it just runs the fat jar
# produced by the previous stage.
FROM amazoncorretto:25.0.3-alpine@sha256:32d81edae73e1670244827c2f12e5bcf0d335f035b538455fe9d02eb0771d41b

# Create a non-root user
RUN adduser -D -h /home/testuser testuser

USER root
RUN mkdir /target && chown testuser /target

# Copy required files from build stage into image
COPY --from=build --chown=testuser /build/target/ /home/testuser/target
COPY --from=build --chown=testuser /root/.m2/ /home/testuser/.m2/
COPY --chown=testuser target/pom.xml /home/testuser/pom.xml
COPY --chown=testuser target/.mvn /home/testuser/.mvn
COPY --chown=testuser target/mvnw /home/testuser

USER testuser

WORKDIR /home/testuser

ENV E2E_SCREENSHOT_DIR /target

ENTRYPOINT ["./mvnw", "-B", "-o", "-DreportsDirectory=/target/surefire-reports", "test"]