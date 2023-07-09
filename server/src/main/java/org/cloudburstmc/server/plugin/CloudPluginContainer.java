package org.cloudburstmc.server.plugin;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Objects;

public class CloudPluginContainer implements PluginContainer {

    private final Object plugin;
    private final PluginDescription description;
    private final Logger logger;
    private final Path dataDirectory;

    public CloudPluginContainer(Object plugin, PluginDescription description, Logger logger, Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.description = Objects.requireNonNull(description, "description");
        ;
        this.logger = Objects.requireNonNull(logger, "logger");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    }

    @NonNull
    @Override
    public Object getPlugin() {
        return plugin;
    }

    @NonNull
    @Override
    public PluginDescription getDescription() {
        return description;
    }

    @NonNull
    @Override
    public Logger getLogger() {
        return logger;
    }

    @NonNull
    @Override
    public Path getDataDirectory() {
        return dataDirectory;
    }
}
