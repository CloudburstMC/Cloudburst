package org.cloudburstmc.server.configuration;

import lombok.SneakyThrows;
import org.cloudburstmc.server.configuration.type.LevelDimension;
import org.cloudburstmc.server.level.generator.GeneratorConfigurationMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalConfigurationMappingTest {

    @Test
    @SneakyThrows
    void parsingWorks() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("config/cloudburst.yml");
        GlobalConfiguration yml = GeneratorConfigurationMapper.MAPPER.readValue(stream, GlobalConfiguration.class);

        assertEquals(
                GlobalConfiguration.Settings.builder()
                        .language("en_US")
                        .forceLanguage(false)
                        .shutdownMessage("Server closed")
                        .deprecatedVerbose(true)
                        .build(),
                yml.getSettings()
        );

        assertEquals(
                GlobalConfiguration.Network.builder()
                        .identityFile("keys/host.pem")
                        .advertisedAddresses(List.of("203.0.113.10:19132", "[2001:db8::1]:19132"))
                        .tlsCertificate("tls/fullchain.pem")
                        .tlsPrivateKey("tls/private.pem")
                        .maxConnections(64)
                        .maxConnectionsPerAddress(4)
                        .maxPendingJoins(16)
                        .handshakeTimeoutSeconds(20)
                        .loginTimeoutSeconds(90)
                        .build(),
                yml.getNetwork()
        );

        assertEquals(
                GlobalConfiguration.Debug.builder()
                        .level(1)
                        .commands(false)
                        .uncaughtExceptionReports(false)
                        .ignoredPackets(Stream.of("LevelChunkPacket").collect(Collectors.toList()))
                        .build(),
                yml.getDebug()
        );

        assertEquals(
                GlobalConfiguration.Timings.builder()
                        .enabled(false)
                        .verbose(false)
                        .historyInterval(6000)
                        .historyLength(72000)
                        .bypassMax(false)
                        .privacy(false)
                        .ignore(Collections.emptyList())
                        .build(),
                yml.getTimings()
        );

        assertEquals(
                GlobalConfiguration.LevelStorage.builder()
                        .defaultFormat("leveldb")
                        .build(),
                yml.getLevelStorage()
        );

        assertEquals(
                GlobalConfiguration.ChunkGeneration.builder()
                        .loadConcurrency(4)
                        .generationConcurrency(0)
                        .saveConcurrency(8)
                        .build(),
                yml.getChunkGeneration()
        );

        assertEquals(2, yml.getChunkSystem().getIoThreads());
        assertEquals(3, yml.getChunkSystem().getWorkerThreads());

        assertEquals(
                GlobalConfiguration.TicksPer.builder()
                        .animalSpawns(400)
                        .monsterSpawns(1)
                        .autosave(6000)
                        .cacheCleanup(900)
                        .build(),
                yml.getTicksPer()
        );

        assertEquals(
                GlobalConfiguration.Player.builder()
                        .savePlayerData(true)
                        .skinChangeCooldown(30)
                        .build(),
                yml.getPlayer()
        );

        HashMap<String, LevelDefinition> levelDefinitions = new HashMap<>();
        levelDefinitions.put("world", new LevelDefinition(
                LevelDimension.OVERWORLD,
                "test",
                "cloudburst:standard",
                "overworld",
                null
        ));

        levelDefinitions.put("nether", new LevelDefinition(
                LevelDimension.NETHER,
                null,
                "cloudburst:standard",
                "nether",
                null
        ));

        assertEquals(
                levelDefinitions,
                yml.getLevels()
        );

        LevelDefinition nether = yml.getLevels().get("nether");
        assertNull(nether.getSeed());
    }
}
