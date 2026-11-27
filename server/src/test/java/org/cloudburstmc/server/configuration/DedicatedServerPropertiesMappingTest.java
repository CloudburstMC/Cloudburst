package org.cloudburstmc.server.configuration;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DedicatedServerPropertiesMappingTest {

    private final DedicatedServerProperties properties = DedicatedServerProperties.builder()
            .motd("A Cloudburst Powered Server")
            .subMotd("https://cloudburstmc.org")
            .serverIp("")
            .serverPort(19132)
            .onlineMode(true)
            .maxPlayers(20)
            .viewDistance(10)
            .simulationDistance(10)
            .whitelist(false)
            .enforceWhitelist(false)
            .levelName("world")
            .gamemode("survival")
            .forceGamemode(false)
            .difficulty("easy")
            .hardcore(false)
            .spawnProtection(16)
            .opPermissionLevel(4)
            .allowFlight(false)
            .requireResourcePack(false)
            .playerIdleTimeout(0)
            .networkCompressionThreshold(1)
            .rateLimit(0)
            .logIps(true)
            .maxTickTime(60000)
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
