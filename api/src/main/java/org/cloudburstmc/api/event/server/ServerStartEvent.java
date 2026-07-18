package org.cloudburstmc.api.event.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.event.Event;

/**
 * Called when server startup has completed.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerStartEvent extends Event {

    /**
     * The shared event instance.
     */
    public static final ServerStartEvent INSTANCE = new ServerStartEvent();
}
