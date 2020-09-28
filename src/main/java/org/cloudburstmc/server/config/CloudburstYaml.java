package org.cloudburstmc.server.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.config.serializer.WorldConfigDeserializer;

import java.nio.file.Path;
import java.util.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CloudburstYaml {

    @SneakyThrows
    public static CloudburstYaml fromFile(Path file) {
        final CloudburstYaml yaml = new CloudburstYaml();
        CloudburstYaml mapped = Bootstrap.KEBAB_CASE_YAML_MAPPER.readerForUpdating(yaml).readValue(file.toFile());
        //fix: when writing commandAlias in yaml but have no item, mapper will treat it as null
        if(mapped.getAliases() == null) {
            mapped = new CloudburstYaml(
                    Collections.emptyMap(),
                    mapped.timings,
                    mapped.settings,
                    mapped.network,
                    mapped.levelSettings,
                    mapped.chunkSending,
                    mapped.chunkTicking,
                    mapped.chunkGeneration,
                    mapped.spawnLimits,
                    mapped.ticksPer,
                    mapped.debug,
                    mapped.player,
                    mapped.worlds
            );
        }
        return mapped;
    }

    @Builder.Default
    private Map<String, List<String>> aliases = new HashMap<>();

    @Builder.Default
    private ServerConfig.Timings timings = new ServerConfig.Timings();

    @Builder.Default
    private ServerConfig.Settings settings = new ServerConfig.Settings();

    @Builder.Default
    private ServerConfig.Network network = new ServerConfig.Network();

    @Builder.Default
    private ServerConfig.LevelSettings levelSettings = new ServerConfig.LevelSettings();

    @Builder.Default
    private ServerConfig.ChunkSending chunkSending = new ServerConfig.ChunkSending();

    @Builder.Default
    private ServerConfig.ChunkTicking chunkTicking = new ServerConfig.ChunkTicking();

    @Builder.Default
    private ServerConfig.ChunkGeneration chunkGeneration = new ServerConfig.ChunkGeneration();

    @Builder.Default
    private ServerConfig.SpawnLimits spawnLimits = new ServerConfig.SpawnLimits();

    @Builder.Default
    private ServerConfig.TicksPer ticksPer = new ServerConfig.TicksPer();

    @Builder.Default
    private ServerConfig.Debug debug = new ServerConfig.Debug();

    @Builder.Default
    private ServerConfig.Player player = new ServerConfig.Player();

    @Builder.Default
    @JsonDeserialize(using=WorldConfigDeserializer.class)
    private Map<String, ServerConfig.World> worlds = new HashMap<>();

    public ObjectNode getRootNode() {
        return Bootstrap.KEBAB_CASE_YAML_MAPPER.valueToTree(this);
    }

}
