package org.cloudburstmc.server.config;

import lombok.SneakyThrows;
import org.cloudburstmc.server.Bootstrap;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CloudburstYamlMappingTest {

    @Test
    @SneakyThrows
    void parsingWorks() {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream("config/cloudburst.yml");
        final CloudburstYaml yml = Bootstrap.KEBAB_CASE_YAML_MAPPER.readValue(stream, CloudburstYaml.class);

        assertEquals(
                SettingsConfig.builder()
                        .language("en_US")
                        .forceLanguage(false)
                        .shutdownMessage("Server closed")
                        .queryPlugins(true)
                        .asyncWorkers("auto")
                        .deprecatedVerbose(true)
                        .build(),
                yml.getSettingsConfig()
        );

        assertEquals(
                NetworkConfig.builder()
                        .batchThreshold(256)
                        .compressionLevel(7)
                        .asyncCompression(false)
                        .build(),
                yml.getNetworkConfig()
        );

        assertEquals(
                DebugConfig.builder()
                        .level(1)
                        .commands(false)
                        .bugReport(false)
                        .ignoredPackets(Stream.of("LevelChunkPacket").collect(Collectors.toList()))
                        .build(),
                yml.getDebugConfig()
        );

        assertEquals(
                TimingsConfig.builder()
                        .enabled(false)
                        .verbose(false)
                        .historyInterval(6000)
                        .historyLength(72000)
                        .bypassMax(false)
                        .privacy(false)
                        .ignore(Collections.emptyList())
                        .build(),
                yml.getTimingsConfig()
        );

        assertEquals(
                LevelSettingsConfig.builder()
                        .defaultFormat("leveldb")
                        .autoTickRate(true)
                        .autoTickRateLimit(20)
                        .baseTickRate(1)
                        .alwaysTickPlayers(false)
                        .chunkTimeoutAfterLoad(30)
                        .chunkTimeoutAfterLastAccess(120)
                        .build(),
                yml.getLevelSettingsConfig()
        );

        assertEquals(
                ChunkSendingConfig.builder()
                        .perTick(4)
                        .maxChunkRadius(48)
                        .spawnThreshold(56)
                        .cacheChunks(false)
                        .build(),
                yml.getChunkSendingConfig()
        );

        assertEquals(
                ChunkTickingConfig.builder()
                        .perTick(40)
                        .tickRadius(3)
                        .lightUpdates(false)
                        .clearTickList(false)
                        .build(),
                yml.getChunkTickingConfig()
        );

        assertEquals(
                ChunkGenerationConfig.builder()
                        .queueSize(8)
                        .populationQueueSize(8)
                        .build(),
                yml.getChunkGenerationConfig()
        );

        assertEquals(
                TicksPerConfig.builder()
                        .animalSpawns(400)
                        .monsterSpawns(1)
                        .autosave(6000)
                        .cacheCleanup(900)
                        .build(),
                yml.getTicksPerConfig()
        );

        assertEquals(
                SpawnLimitsConfig.builder()
                        .monsters(70)
                        .animals(15)
                        .waterAnimals(5)
                        .ambient(15)
                        .build(),
                yml.getSpawnLimitsConfig()
        );

        assertEquals(
                PlayerConfig.builder()
                        .savePlayerData(true)
                        .skinChangeCooldown(30)
                        .build(),
                yml.getPlayerConfig()
        );

        HashMap<String, List<String>> aliases = new HashMap<>();
        aliases.put("savestop", Stream.of("save-all", "stop").collect(Collectors.toList()));
        assertEquals(
                aliases,
                yml.getCommandAliases()
        );

        HashMap<String, WorldConfig> worldConfig = new HashMap<>();
        worldConfig.put("world", new WorldConfig(
                "test",
                "cloudburst:standard",
                "overworld"
        ));
        worldConfig.put("nether", new WorldConfig(
                "nether",
                "cloudburst:standard",
                "nether"
        ));
        assertEquals(
                worldConfig,
                yml.getWorldConfig()
        );
    }

}
