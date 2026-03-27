FROM eclipse-temurin:25-jdk-alpine AS build

RUN apk add --no-cache git

WORKDIR /src

# Gradle wrapper and build scripts copied first so dependency layers cache
# independently from source changes
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY api ./api
COPY codegen ./codegen
COPY server ./server
COPY vanilla ./vanilla

# Required so generateGitProperties can embed the commit hash and branch
COPY .git ./.git

RUN chmod +x gradlew

# Cache mount keeps the Gradle distribution and all dependencies between builds
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :server:shadowJar --no-daemon

FROM eclipse-temurin:25-jre-alpine AS run

ARG VERSION=dev

LABEL org.opencontainers.image.title="Cloudburst" \
      org.opencontainers.image.description="Cloudburst Minecraft: Bedrock Edition server" \
      org.opencontainers.image.source="https://github.com/CloudburstMC/Server" \
      org.opencontainers.image.licenses="LGPL-3.0" \
      org.opencontainers.image.version="${VERSION}"

RUN mkdir /app \
    && addgroup -S cloudburst && adduser -S -G cloudburst \
        -h /data -s /sbin/nologin cloudburst \
    && chown cloudburst:cloudburst /app

COPY --from=build /src/server/build/libs/Cloudburst.jar /app/cloudburst.jar

EXPOSE 19132/udp

USER cloudburst:cloudburst

WORKDIR /data

VOLUME /data

ENTRYPOINT ["java", "-jar", "/app/cloudburst.jar"]
CMD ["--language", "en_US"]
