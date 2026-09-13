package org.cloudburstmc.api.event.player;

/**
 * The cause of a player teleport.
 */
public enum PlayerTeleportCause {
    /**
     * A teleport initiated by a command.
     */
    COMMAND,
    /**
     * A teleport initiated by a plugin.
     */
    PLUGIN,
    /**
     * A teleport through a Nether portal.
     */
    NETHER_PORTAL,
    /**
     * A teleport through an End portal.
     */
    END_PORTAL,
    /**
     * A teleport through an End gateway.
     */
    END_GATEWAY,
    /**
     * A teleport caused by an ender pearl.
     */
    ENDER_PEARL,
    /**
     * A teleport caused by eating chorus fruit.
     */
    CHORUS_FRUIT,
    /**
     * A teleport whose cause is not known.
     */
    UNKNOWN
}
