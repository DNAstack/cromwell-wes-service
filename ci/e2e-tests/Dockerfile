# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

# This stage depends on a JDK image and uses Maven Wrapper to pull in dependencies and build the project
# from source.
FROM amazoncorretto:17-alpine AS build
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

# Now add sources, which will bust the cache.
ADD target/src ./src

# If this step fails with an error like
#     Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:2.22.1:test
#     (default-test) on project xxx: Unable to generate classpath:
#     org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException: Missing:
# then update the version number of surefire-junit4 in pom.xml under the
# dynamicDependencies section of go-offline-maven-plugin configuration.
RUN ./mvnw -P -dnastack -B -o package


##
## Stage 2: Package the runnable image
##

# This stage uses a JRE rather than JDK because it doesn't have to build anything; it just runs the fat jar
# produced by the previous stage.
FROM amazoncorretto:17-alpine
ARG APP_NAME
ARG APP_VERSION

ADD target/e2e-exec-pom.xml /pom.xml
ADD target/.mvn /.mvn
ADD target/mvnw /
COPY --from=0 /build/target/wes-service-e2e-tests-*.jar /e2e-tests.jar
RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

# If the container has a memory limit, that limit (in bytes) is present in a file under /sys/fs/cgroup
# In Linux 5.10.76, the value comes from memory.max.
# If the container doesn't have a set memory limit, the file contains "max", hence we fall back to a default value of 2GiB
# In Linux 5.4.129, the value comes from memory/memory.limit_in_bytes
# If the container doesn't have a set memory limit, the file will be blank, hence we fall back to a default value of 2Gib
ENTRYPOINT ["./mvnw", "-B", "-o", "test"]