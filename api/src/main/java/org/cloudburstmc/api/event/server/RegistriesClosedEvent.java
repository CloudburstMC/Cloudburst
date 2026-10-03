package org.cloudburstmc.api.event.server;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.registry.ResourcePackRegistry;

/**
 * Fired immediately after the registries are closed.
 */
@RequiredArgsConstructor
public class RegistriesClosedEvent extends ServerEvent {

    @NonNull
    private final ResourcePackRegistry resourcePackRegistry;

    /**
     * Returns the resource pack registry after registration has closed.
     *
     * @return the resource pack registry
     */
    @NonNull
    public ResourcePackRegistry getResourcePackRegistry() {
        return this.resourcePackRegistry;
    }
}
