package org.cloudburstmc.api.event.server;

import org.cloudburstmc.api.event.Event;

/**
 * Base class for server events.
 */
public abstract class ServerEvent extends Event {

    protected ServerEvent() {
        super();
    }

    protected ServerEvent(boolean asynchronous) {
        super(asynchronous);
    }
}
