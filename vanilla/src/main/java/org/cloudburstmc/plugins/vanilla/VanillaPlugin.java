package org.cloudburstmc.plugins.vanilla;

import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.event.Listener;
import org.cloudburstmc.api.event.server.ServerShutdownEvent;
import org.cloudburstmc.api.event.server.ServerStartEvent;
import org.cloudburstmc.api.plugin.Plugin;

import javax.inject.Inject;

@Plugin(
        name = "Vanilla",
        authors = "CloudburstMC",
        id = "vanilla",
        url = "https://github.com/CloudburstMC/Vanilla",
        version = "0.0.1-SNAPSHOT"
)
public class VanillaPlugin {

    private final Server server;

    @Inject
    public VanillaPlugin(Server server) {
        this.server = server;
    }

    @Listener
    public void onServerStart(ServerStartEvent event) {

    }

    @Listener
    public void onServerShutdown(ServerShutdownEvent event) {

    }
}
