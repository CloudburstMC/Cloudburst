package org.cloudburstmc.api.event.server;

import java.util.Objects;

/**
 * Fired on the server thread after startup or a full server reload has completed.
 * Reloading configuration settings alone does not fire this event.
 */
public class ServerLoadEvent extends ServerEvent {
    private final ServerLoadType type;

    /**
     * Creates a notification for a completed server load.
     *
     * @param type whether startup or a full reload completed
     */
    public ServerLoadEvent(ServerLoadType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Returns whether startup or a full reload completed.
     *
     * @return the completed load type
     */
    public ServerLoadType getType() {
        return this.type;
    }
}
