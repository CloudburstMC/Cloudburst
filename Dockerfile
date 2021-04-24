# This Dockerfile uses Docker Multi-Stage Builds
# See https://docs.docker.com/engine/userguide/eng-image/multistage-build/
# Requires Docker v17.05

# Use maven java 16 image for intermiediate build
FROM maven:3-openjdk-16-slim AS build

# Install packages required for build
RUN apt update && apt install -y git

# Build from source and create artifact
WORKDIR /src
COPY ./ /src
RUN git submodule update --init
RUN mvn clean package

# Use OpenJDK JRE image for runtime
FROM openjdk:16-jdk-slim AS run

# Copy artifact from build image
COPY --from=build /src/target/Cloudburst.jar /app/Cloudburst.jar

# Create minecraft user
RUN useradd --user-group \
            --no-create-home \
            --home-dir /data \
            --shell /usr/sbin/nologin \
            minecraft

# Volumes
VOLUME /data /home/minecraft

# Ports
EXPOSE 19132

# Make app owned by minecraft user
RUN chown -R minecraft:minecraft /app

# User and group to run as
USER minecraft:minecraft

# Set runtime workdir
WORKDIR /data

# Run app
ENTRYPOINT ["java"]
CMD [ "-jar", "--enable-preview", "/app/Cloudburst.jar" ]
