package org.cloudburstmc.api.event.server;

import org.cloudburstmc.api.registry.ResourcePackRegistry;

import java.util.Objects;

/**
 * Fired on the server thread immediately after the registries are closed.
 */
public class RegistriesClosedEvent extends ServerEvent {

    private final ResourcePackRegistry resourcePackRegistry;

    /**
     * Creates a notification that registration has closed.
     *
     * @param resourcePackRegistry the closed resource pack registry
     */
    public RegistriesClosedEvent(ResourcePackRegistry resourcePackRegistry) {
        this.resourcePackRegistry = Objects.requireNonNull(resourcePackRegistry, "resourcePackRegistry");
    }

    /**
     * Returns the resource pack registry after registration has closed.
     *
     * @return the resource pack registry
     */
    public ResourcePackRegistry getResourcePackRegistry() {
        return this.resourcePackRegistry;
    }
}
