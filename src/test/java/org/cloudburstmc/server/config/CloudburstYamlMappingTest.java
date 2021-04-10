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
                ServerConfig.Settings.builder()
                        .language("en_US")
                        .forceLanguage(false)
                        .shutdownMessage("Server closed")
                        .queryPlugins(true)
                        .asyncWorkers("auto")
                        .deprecatedVerbose(true)
                        .build(),
                yml.getSettings()
        );

        assertEquals(
                ServerConfig.Network.builder()
                        .batchThreshold(256)
                        .compressionLevel(7)
                        .asyncCompression(false)
                        .build(),
                yml.getNetwork()
        );

        assertEquals(
                ServerConfig.Debug.builder()
                        .level(1)
                        .commands(false)
                        .bugReport(false)
                        .ignoredPackets(Stream.of("LevelChunkPacket").collect(Collectors.toList()))
                        .build(),
                yml.getDebug()
        );

        assertEquals(
                ServerConfig.Timings.builder()
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
                ServerConfig.LevelSettings.builder()
                        .defaultFormat("leveldb")
                        .autoTickRate(true)
                        .autoTickRateLimit(20)
                        .baseTickRate(1)
                        .alwaysTickPlayers(false)
                        .chunkTimeoutAfterLoad(30)
                        .chunkTimeoutAfterLastAccess(120)
                        .build(),
                yml.getLevelSettings()
        );

        assertEquals(
                ServerConfig.ChunkSending.builder()
                        .perTick(4)
                        .maxChunkRadius(48)
                        .spawnThreshold(56)
                        .cacheChunks(false)
                        .build(),
                yml.getChunkSending()
        );

        assertEquals(
                ServerConfig.ChunkTicking.builder()
                        .perTick(40)
                        .tickRadius(3)
                        .lightUpdates(false)
                        .clearTickList(false)
                        .build(),
                yml.getChunkTicking()
        );

        assertEquals(
                ServerConfig.ChunkGeneration.builder()
                        .queueSize(8)
                        .populationQueueSize(8)
                        .build(),
                yml.getChunkGeneration()
        );

        assertEquals(
                ServerConfig.TicksPer.builder()
                        .animalSpawns(400)
                        .monsterSpawns(1)
                        .autosave(6000)
                        .cacheCleanup(900)
                        .build(),
                yml.getTicksPer()
        );

        assertEquals(
                ServerConfig.SpawnLimits.builder()
                        .monsters(70)
                        .animals(15)
                        .waterAnimals(5)
                        .ambient(15)
                        .build(),
                yml.getSpawnLimits()
        );

        assertEquals(
                ServerConfig.Player.builder()
                        .savePlayerData(true)
                        .skinChangeCooldown(30)
                        .build(),
                yml.getPlayer()
        );

        HashMap<String, List<String>> aliases = new HashMap<>();
        aliases.put("savestop", Stream.of("save-all", "stop").collect(Collectors.toList()));
        assertEquals(
                aliases,
                yml.getAliases()
        );

      /*HashMap<String, ServerConfig.World> worldConfig = new HashMap<>(); With the new system, the old test that checks for seeds is obsolute
        worldConfig.put("world", new ServerConfig.World(
                "test",
                "cloudburst:standard",
                "overworld"
        ));
        worldConfig.put("nether", new ServerConfig.World(
                "nether",
                "cloudburst:standard",
                "nether"
        ));
        assertEquals(
                worldConfig,
                yml.getWorlds()
        );*/
    }

}
