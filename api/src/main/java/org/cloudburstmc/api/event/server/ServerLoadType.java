package org.cloudburstmc.api.event.server;

/**
 * The lifecycle operation completed by a {@link ServerLoadEvent}.
 */
public enum ServerLoadType {
    /**
     * Initial server startup completed.
     */
    STARTUP,
    /**
     * A full server reload completed, rather than a settings-only reload.
     */
    RELOAD
}
