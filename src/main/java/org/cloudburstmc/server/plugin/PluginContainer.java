package org.cloudburstmc.server.plugin;

import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.nio.file.Path;

public interface PluginContainer extends PluginDescription {
    @Nonnull
    Object getPlugin();

    @Nonnull
    Logger getLogger();

    InputStream getResource(@Nonnull String name);

    Path getDirectory();
}
