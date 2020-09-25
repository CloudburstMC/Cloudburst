package org.cloudburstmc.server.config;

import org.cloudburstmc.server.utils.Config;

import java.nio.file.Path;

/**
 * represents the cloudburst.yml file on disk
 */
public class CloudburstYaml {
    
    private final Path location;

    private final Config config;

    public CloudburstYaml(Path location) {
        this.location = location;
        this.config = new Config(location.toString(), Config.YAML);
    }

    // generic getter //

    public Path getLocation() {
        return location;
    }

    public Config getRawConfig() {
        return config;
    }

    public <T> T getConfig(String variable) {
        return this.getConfig(variable, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String variable, T defaultValue) {
        Object value = this.config.get(variable);
        return value == null ? defaultValue : (T) value;
    }

}
