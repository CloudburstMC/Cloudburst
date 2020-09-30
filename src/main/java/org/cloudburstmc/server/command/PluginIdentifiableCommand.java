package org.cloudburstmc.server.command;

import org.cloudburstmc.server.plugin.PluginContainer;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface PluginIdentifiableCommand {

    /**
     * Returns the owner of the PluginIdentifiableCommand.
     *
     * @return The plugin that owns this PluginIdentifiableCommand
     */
    PluginContainer getPlugin();
}
