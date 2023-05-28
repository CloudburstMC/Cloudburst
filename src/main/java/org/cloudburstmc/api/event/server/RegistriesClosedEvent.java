package org.cloudburstmc.api.event.server;

import org.cloudburstmc.api.registry.ResourcePackRegistry;

/**
 * Fired immediately after the registries are closed.
 *
 * @author DaPorkchop_
 */
public final class RegistriesClosedEvent extends ServerEvent {

    private final ResourcePackRegistry packManager;

    public RegistriesClosedEvent(ResourcePackRegistry packManager) {
        this.packManager = packManager;
    }

    public ResourcePackRegistry getPackManager() {
        return this.packManager;
    }
}
