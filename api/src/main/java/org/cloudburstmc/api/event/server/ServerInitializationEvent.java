package org.cloudburstmc.api.event.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.event.Event;

/**
 * Called after plugins load and before registries close.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerInitializationEvent extends Event {

    /**
     * The shared event instance.
     */
    public static final ServerInitializationEvent INSTANCE = new ServerInitializationEvent();
}
