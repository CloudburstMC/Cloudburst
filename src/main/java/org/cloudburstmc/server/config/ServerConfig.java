package org.cloudburstmc.server.config;

import java.util.*;

/**
 * the universal public facing facade for the server's config
 * so you dont have to care about where the config actually lies
 */
public class ServerConfig {

    private final ServerProperties properties;

    private final CloudburstYaml cloudburstYaml;

    public ServerConfig(ServerProperties properties, CloudburstYaml cloudburstYaml) {
        this.properties = properties;
        this.cloudburstYaml = cloudburstYaml;
    }

    // forwarding server.properties

    public PropertiesConfig getPropertiesConfig() {
        return new PropertiesConfig(properties);
    }

    // forwarding cloudburst.yml //

    public Map<String, List<String>> getCommandAliases() {
        return cloudburstYaml.getCommandAliases();
    }

    public TimingsConfig getTimingsConfig() {
        return cloudburstYaml.getTimingsConfig();
    }

    public SettingsConfig getSettingsConfig() {
        return cloudburstYaml.getSettingsConfig();
    }

    public NetworkConfig getNetworkConfig() {
        return cloudburstYaml.getNetworkConfig();
    }

    public LevelSettingsConfig getLevelSettingsConfig() {
        return cloudburstYaml.getLevelSettingsConfig();
    }

    public ChunkSendingConfig getChunkSendingConfig() {
        return cloudburstYaml.getChunkSendingConfig();
    }

    public ChunkTickingConfig getChunkTickingConfig() {
        return cloudburstYaml.getChunkTickingConfig();
    }

    public TicksPerConfig getTicksPerConfig() {
        return cloudburstYaml.getTicksPerConfig();
    }

    public DebugConfig getDebugConfig() {
        return cloudburstYaml.getDebugConfig();
    }

    public PlayerConfig getPlayerConfig() {
        return cloudburstYaml.getPlayerConfig();
    }

    public Map<String, WorldConfig> getWorldConfig() {
        return cloudburstYaml.getWorldConfig();
    }

    // escape hatch //

    public ServerProperties getServerProperties() {
        return properties;
    }

    public CloudburstYaml getCloudburstYaml() { return cloudburstYaml; }

}
