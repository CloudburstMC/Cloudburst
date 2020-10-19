package org.cloudburstmc.server.plugin;

import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public interface PluginContainer {

    @Nonnull
    Object getPlugin();

    @Nonnull
    PluginDescription getDescription();

    @Nonnull
    Logger getLogger();

    @Nonnull
    Path getDataDirectory();
}
