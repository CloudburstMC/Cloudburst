package org.cloudburstmc.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        if(mapped.getCommandAliases() == null) {
            mapped = new CloudburstYaml(
                    Collections.emptyMap(),
                    mapped.timingsConfig,
                    mapped.settingsConfig,
                    mapped.networkConfig,
                    mapped.levelSettingsConfig,
                    mapped.chunkSendingConfig,
                    mapped.chunkTickingConfig,
                    mapped.chunkGenerationConfig,
                    mapped.spawnLimitsConfig,
                    mapped.ticksPerConfig,
                    mapped.debugConfig,
                    mapped.playerConfig,
                    mapped.worldConfig
            );
        }
        return mapped;
    }

    @Builder.Default
    @JsonProperty("aliases")
    private Map<String, List<String>> commandAliases = new HashMap<>();

    @Builder.Default
    @JsonProperty("timings")
    private TimingsConfig timingsConfig = new TimingsConfig();

    @Builder.Default
    @JsonProperty("settings")
    private SettingsConfig settingsConfig = new SettingsConfig();

    @Builder.Default
    @JsonProperty("network")
    private NetworkConfig networkConfig = new NetworkConfig();

    @Builder.Default
    @JsonProperty("level-settings")
    private LevelSettingsConfig levelSettingsConfig = new LevelSettingsConfig();

    @Builder.Default
    @JsonProperty("chunk-sending")
    private ChunkSendingConfig chunkSendingConfig = new ChunkSendingConfig();

    @Builder.Default
    @JsonProperty("chunk-ticking")
    private ChunkTickingConfig chunkTickingConfig = new ChunkTickingConfig();

    @Builder.Default
    @JsonProperty("chunk-generation")
    private ChunkGenerationConfig chunkGenerationConfig = new ChunkGenerationConfig();

    @Builder.Default
    @JsonProperty("spawn-limits")
    private SpawnLimitsConfig spawnLimitsConfig = new SpawnLimitsConfig();

    @Builder.Default
    @JsonProperty("ticks-per")
    private TicksPerConfig ticksPerConfig = new TicksPerConfig();

    @Builder.Default
    @JsonProperty("debug")
    private DebugConfig debugConfig = new DebugConfig();

    @Builder.Default
    @JsonProperty("player")
    private PlayerConfig playerConfig = new PlayerConfig();

    @Builder.Default
    @JsonProperty("worlds")
    @JsonDeserialize(using= WorldConfigDeserializer.class)
    private Map<String, WorldConfig> worldConfig = new HashMap<>();

    public ObjectNode getRootNode() {
        return Bootstrap.KEBAB_CASE_YAML_MAPPER.valueToTree(this);
    }

}
