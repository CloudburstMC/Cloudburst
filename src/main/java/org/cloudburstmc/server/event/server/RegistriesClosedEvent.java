package org.cloudburstmc.server.event.server;

import org.cloudburstmc.server.pack.PackManager;

/**
 * Fired immediately after the registries are closed.
 *
 * @author DaPorkchop_
 */
public class RegistriesClosedEvent extends ServerEvent {

    private final PackManager packManager;

    public RegistriesClosedEvent(PackManager packManager) {
        this.packManager = packManager;
    }

    public PackManager getPackManager() {
        return this.packManager;
    }
}
