package org.cloudburstmc.api.event.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.registry.ResourcePackRegistry;

/**
 * Fired immediately after the registries are closed.
 */
@Getter
@RequiredArgsConstructor
public final class RegistriesClosedEvent extends ServerEvent {

    @NonNull
    private final ResourcePackRegistry resourcePackRegistry;
}
