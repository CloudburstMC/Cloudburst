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

    String serialized = "motd=A Cloudburst Powered Server\n" +
            "sub-motd=https://cloudburstmc.org\n" +
            "server-ip=0.0.0.0\n" +
            "server-port=19132\n" +
            "view-distance=10\n" +
            "white-list=false\n" +
            "achievements=true\n" +
            "announce-player-achievements=true\n" +
            "spawn-protection=16\n" +
            "max-players=20\n" +
            "allow-flight=false\n" +
            "spawn-animals=true\n" +
            "spawn-mobs=true\n" +
            "gamemode=0\n" +
            "force-gamemode=false\n" +
            "hardcore=false\n" +
            "pvp=true\n" +
            "difficulty=1\n" +
            "default-level=world\n" +
            "allow-nether=true\n" +
            "enable-query=true\n" +
            "auto-save=true\n" +
            "force-resources=false\n" +
            "xbox-auth=true\n" +
            "generate-structures=true\n"
            ;

    @Test
    @SneakyThrows
    void parsingWorks() {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream("config/server.properties");
        final ServerProperties actual = Bootstrap.JAVA_PROPS_MAPPER.readValue(stream, ServerProperties.class);

        assertEquals(properties, actual);
    }

    @Test
    @SneakyThrows
    void writingWorks() {
        final String actual = Bootstrap.JAVA_PROPS_MAPPER.writeValueAsString(properties);
        assertEquals(serialized, actual);
    }

}
