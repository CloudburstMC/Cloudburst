package org.cloudburstmc.server.config;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.node.ObjectNode;
import lombok.*;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.config.serializer.WorldConfigDeserializer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class CloudburstYaml {

    public static CloudburstYaml fromFile(Path file) {
        final CloudburstYaml yaml = new CloudburstYaml();
        try (InputStream stream = Files.newInputStream(file)) {
            return Bootstrap.YAML_MAPPER.readerForUpdating(yaml).readValue(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read configuration file", e);
        }
    }

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
    private ServerConfig.Movement movement = new ServerConfig.Movement();

    @Builder.Default
    private ServerConfig.Interaction interaction = new ServerConfig.Interaction();

    @Builder.Default
    private ServerConfig.Level level = new ServerConfig.Level();

    @Builder.Default
    @JsonDeserialize(using=WorldConfigDeserializer.class)
    private Map<String, ServerConfig.World> worlds = new HashMap<>();

    public ObjectNode getRootNode() {
        return Bootstrap.YAML_MAPPER.valueToTree(this);
    }

}
