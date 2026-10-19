package org.cloudburstmc.server.configuration;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DedicatedServerPropertiesMappingTest {

    private final DedicatedServerProperties properties = DedicatedServerProperties.builder()
            .achievements(true)
            .defaultLevel("world")
            .viewDistance(10)
            .serverIp("0.0.0.0")
            .serverPort(19132)
            .gamemode(0)
            .allowNether(true)
            .forceResources(false)
            .autoSave(true)
            .motd("A Cloudburst Powered Server")
            .announcePlayerAchievements(true)
            .forceGamemode(false)
            .hardcore(false)
            .whiteList(false)
            .xboxAuth(true)
            .pvp(true)
            .spawnMobs(true)
            .spawnAnimals(true)
            .difficulty(1)
            .subMotd("https://cloudburstmc.org")
            .maxPlayers(20)
            .spawnProtection(16)
            .allowFlight(false)
            .build();

    @Test
    @SneakyThrows
    void parsingWorks() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("config/server.properties");
        DedicatedServerProperties actual = ServerConfigurationMappers.PROPERTIES.readValue(stream, DedicatedServerProperties.class);
        assertEquals(properties, actual);
    }

    @Test
    @SneakyThrows
    void writingRoundTrips() {
        String serialized = ServerConfigurationMappers.PROPERTIES.writeValueAsString(properties);
        DedicatedServerProperties actual = ServerConfigurationMappers.PROPERTIES.readValue(serialized, DedicatedServerProperties.class);
        assertEquals(properties, actual);
    }
}
