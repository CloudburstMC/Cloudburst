package org.cloudburstmc.api.event.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.event.Event;

/**
 * Called while the server is shutting down.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerShutdownEvent extends Event {

    /**
     * The shared event instance.
     */
    public static final ServerShutdownEvent INSTANCE = new ServerShutdownEvent();
}
