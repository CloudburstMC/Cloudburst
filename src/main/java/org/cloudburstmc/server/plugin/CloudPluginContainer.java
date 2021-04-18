package org.cloudburstmc.server.plugin;

import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
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

    @Nonnull
    @Override
    public Object getPlugin() {
        return plugin;
    }

    @Nonnull
    @Override
    public PluginDescription getDescription() {
        return description;
    }

    @Nonnull
    @Override
    public Logger getLogger() {
        return logger;
    }

    @Nonnull
    @Override
    public Path getDataDirectory() {
        return dataDirectory;
    }
}
