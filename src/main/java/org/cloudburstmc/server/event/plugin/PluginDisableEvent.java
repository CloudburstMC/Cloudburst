package org.cloudburstmc.server.event.plugin;

import org.cloudburstmc.server.plugin.Plugin;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PluginDisableEvent extends PluginEvent {

    public PluginDisableEvent(Plugin plugin) {
        super(plugin);
    }
}
