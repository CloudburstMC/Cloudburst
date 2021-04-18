package org.cloudburstmc.api.plugin;

import org.slf4j.Logger;

import java.nio.file.Path;

public interface PluginContainer {

    Object getPlugin();

    PluginDescription getDescription();

    Logger getLogger();

    Path getDataDirectory();
}
