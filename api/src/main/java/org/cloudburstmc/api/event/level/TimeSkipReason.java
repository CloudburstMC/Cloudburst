package org.cloudburstmc.api.event.level;

/**
 * The cause of a discontinuous daylight-time change.
 */
public enum TimeSkipReason {
    /**
     * A time command.
     */
    COMMAND,
    /**
     * A change requested through the clock API.
     */
    CUSTOM,
    /**
     * Players sleeping through the night.
     */
    NIGHT_SKIP
}
