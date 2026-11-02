package org.cloudburstmc.api.event.server;

/**
 * Fired on the server thread before a simulation tick begins.
 */
public class ServerTickStartEvent extends ServerEvent {

    private final int tickNumber;

    /**
     * Creates a tick-start notification.
     *
     * @param tickNumber number of the tick about to begin, with the first tick numbered one
     */
    public ServerTickStartEvent(int tickNumber) {
        this.tickNumber = tickNumber;
    }

    /**
     * Returns the number of the tick about to begin.
     *
     * @return the tick number, starting at one
     */
    public int getTickNumber() {
        return this.tickNumber;
    }
}
