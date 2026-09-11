package org.cloudburstmc.server.command.defaults;

/**
 * Determines how {@code setblock} handles the block already at its target position.
 */
public enum SetBlockMode {
    REPLACE,
    DESTROY,
    KEEP
}
