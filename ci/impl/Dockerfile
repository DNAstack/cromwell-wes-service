# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

# This stage depends on a JDK image and uses Maven Wrapper to pull in dependencies and build the project
# from source.
FROM azul/zulu-openjdk:17.0.3 AS build
ARG APP_NAME
ARG APP_VERSION

RUN apt-get update
RUN apt-get install -y curl

WORKDIR /build

# Before bringing in project sources, resolve maven dependencies.
# This allows Docker to reuse the cached image with all dependencies resolved.
ADD target/pom.xml ./
ADD target/.mvn ./.mvn
ADD target/mvnw ./
# Allows us to pass settings.xml configured on local machine or CI server to access private Nexus repo
ADD target/.m2 /root/.m2

# Add the memory calculator to the image
RUN curl -L https://github.com/cloudfoundry/java-buildpack-memory-calculator/releases/download/v4.1.0/memory-calculator-4.1.0.tgz \
    | tar zxv

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
FROM azul/zulu-openjdk:17.0.3-jre
ARG APP_NAME
ARG APP_VERSION

#USER 9001:9001

COPY --from=0 /build/target/*.jar /app.jar
COPY --from=0 /build/java-buildpack-memory-calculator /java-buildpack-memory-calculator
EXPOSE 8090

# If the container has a memory limit, that limit (in bytes) is present in a file under /sys/fs/cgroup
# In Linux 5.10.76, the value comes from memory.max.
# If the container doesn't have a set memory limit, the file contains "max", hence we fall back to a default value of 2GiB
# In Linux 5.4.129, the value comes from memory/memory.limit_in_bytes
# If the container doesn't have a set memory limit, the file will be blank, hence we fall back to a default value of 2GiB
CMD container_mem=$(cat "/sys/fs/cgroup/memory.max" || cat "/sys/fs/cgroup/memory/memory.limit_in_bytes") ; \
    exec java $(/java-buildpack-memory-calculator \
    --total-memory=$([ "$container_mem" = "max" -o -z "$container_mem" -o "$container_mem" -eq 9223372036854771712 ] && echo "2097152000" || echo "$container_mem")B \
    --loaded-class-count="${ESTIMATED_LOADED_CLASSES:-25000}" \
    --thread-count="${ESTIMATED_THREADS:-100}" \
    --jvm-options="${JAVA_OPTS}") ${JAVA_OPTS} \
    -jar app.jar
