package org.cloudburstmc.server.config;

import lombok.SneakyThrows;
import org.cloudburstmc.server.Bootstrap;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerPropertiesMappingTest {

    final ServerProperties properties = ServerProperties.builder()
            .path(null)
            .achievements(true)
            .defaultLevel("world")
            .viewDistance(10)
            .serverIp("0.0.0.0")
            .serverPort(19132)
            .gamemode(0)
            .allowNether(true)
            .enableQuery(true)
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
        final InputStream stream = getClass().getClassLoader().getResourceAsStream("config/server.properties");
        final ServerProperties actual = Bootstrap.JAVA_PROPS_MAPPER.readValue(stream, ServerProperties.class);

        assertEquals(properties, actual);
    }

    @Test
    @SneakyThrows
    void writingRoundTrips() {
        final String serialized = Bootstrap.JAVA_PROPS_MAPPER.writeValueAsString(properties);
        final ServerProperties actual = Bootstrap.JAVA_PROPS_MAPPER.readValue(serialized, ServerProperties.class);

        assertEquals(properties, actual);
    }

}
