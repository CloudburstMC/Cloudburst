![Cloudburst](.github/images/banner.png)

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](server/LICENSE)
[![Build Status](https://github.com/CloudburstMC/Cloudburst/actions/workflows/deploy.yml/badge.svg?branch=develop)](https://github.com/CloudburstMC/Cloudburst/actions/workflows/deploy.yml)
[![Discord](https://img.shields.io/discord/393465748535640064.svg)](https://discord.gg/5PzMkyK)

## Introduction

Cloudburst is a server software for Minecraft: Bedrock Edition.
It has a few key advantages over other server software:

- Written in Java, Cloudburst is faster and more stable.
- Having a friendly structure, it's easy to contribute to Cloudburst's development and rewrite plugins from other platforms into Cloudburst plugins.

Cloudburst is **under improvement**, yet we welcome contributions.

## Links

- **[News](https://cloudburstmc.org)**
- **[Forums](https://cloudburstmc.org/forums)**
- **[Discord](https://discord.gg/5PzMkyK)**
- **[Download](https://dl.opencollab.dev/cloudburst)**
- **[Plugins](https://cloudburstmc.org/resources/categories/cloudburst-plugins.19/)**
- **[Wiki](https://cloudburstmc.org/wiki/cloudburst)**

## Build JAR file

- `git clone https://github.com/CloudburstMC/Cloudburst.git`
- `cd Cloudburst`
- `git submodule update --init`
- `./gradlew shadowJar`

The compiled JAR can be found in the `server/build/libs` directory.

## Running

Simply run `java -jar Cloudburst.jar`.

## Plugin API

Information on Cloudburst's API can be found at the [wiki](https://cloudburstmc.org/wiki/cloudburst/).

## Docker

Run from the repo root:

```
docker compose -f docker/docker-compose.yml up -d
```

The default language is `en_US`. To change it, append `--language <locale>` to the command.

Server data is persisted in the `cloudburst-data` named volume. To access the interactive console after starting:

```
docker attach cloudburst
```

## Contributing

Please read the [CONTRIBUTING](.github/CONTRIBUTING.md) guide before submitting any issue. Issues with insufficient information or in the wrong format will be closed and will not be reviewed.
