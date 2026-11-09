package org.cloudburstmc.server.configuration;

import lombok.SneakyThrows;
import org.cloudburstmc.server.configuration.type.LevelDimension;
import org.cloudburstmc.server.configuration.type.SignalingMode;
import org.cloudburstmc.server.level.generator.GeneratorConfigurationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GlobalConfigurationMappingTest {

    @Test
    void readsExternalSignalingWithoutExposingCredentialsInSettingsDiagnostics(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("cloudburst-global.yml");
        Files.writeString(file, """
                network:
                  signaling-mode: both
                  builtin:
                    media-port: 19132
                    tls-password: test-keystore-password
                    forwarded-for-proxies: [192.0.2.0/24]
                  nxs:
                    endpoint: https://provider.example
                    token: test-provider-token
                    media-port: 19133
                    advertised-addresses: ['203.0.113.10:19133']
                    assisted-joins: true
                """);

        GlobalConfiguration.Network network = GlobalConfiguration.fromFile(file).getNetwork();
        assertEquals(SignalingMode.BOTH, network.getSignalingMode());
        assertEquals(19132, network.getBuiltin().getMediaPort());
        assertEquals(19133, network.getNxs().getMediaPort());
        assertEquals(List.of("192.0.2.0/24"), network.getBuiltin().getForwardedForProxies());
        assertEquals(List.of("203.0.113.10:19133"), network.getNxs().getAdvertisedAddresses());
        assertTrue(network.getNxs().isAssistedJoins());
        assertFalse(network.toString().contains("test-provider-token"));
        assertFalse(network.toString().contains("test-keystore-password"));
    }

    @Test
    @SneakyThrows
    void parsingWorks() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("config/cloudburst.yml");
        GlobalConfiguration yml = GeneratorConfigurationMapper.MAPPER.readValue(stream, GlobalConfiguration.class);

        assertFalse(yml.getSpark().isEnabled());

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
                        .builtin(GlobalConfiguration.BuiltinSignaling.builder()
                                .identityFile("keys/host.pem")
                                .advertisedAddresses(List.of("203.0.113.10:19132", "[2001:db8::1]:19132"))
                                .tlsCertificate("tls/fullchain.pem")
                                .tlsPrivateKey("tls/private.pem")
                                .handshakeTimeoutSeconds(20)
                                .build())
                        .maxConnections(64)
                        .maxConnectionsPerAddress(4)
                        .maxPendingJoins(16)
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
