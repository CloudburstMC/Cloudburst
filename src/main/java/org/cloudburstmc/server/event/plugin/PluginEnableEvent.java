package org.cloudburstmc.server.event.plugin;

import org.cloudburstmc.server.plugin.Plugin;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PluginEnableEvent extends PluginEvent {

    public PluginEnableEvent(Plugin plugin) {
        super(plugin);
    }
}
