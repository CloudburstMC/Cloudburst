package org.cloudburstmc.server.event.server;

import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.pack.PackManager;

/**
 * Fired immediately after the registries are closed.
 *
 * @author DaPorkchop_
 */
public class RegistriesClosedEvent extends ServerEvent {
    private static HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final PackManager packManager;

    public RegistriesClosedEvent(PackManager packManager) {
        this.packManager = packManager;
    }

    public PackManager getPackManager() {
        return this.packManager;
    }
}
