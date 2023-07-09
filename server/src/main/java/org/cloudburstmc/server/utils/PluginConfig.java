package org.cloudburstmc.server.utils;

import com.google.inject.Inject;

import java.nio.file.Path;

public class PluginConfig extends Config {

    @Inject
    public PluginConfig(@ConfigFile Path path) {
        super(path.toFile(), Config.YAML);
    }
}
