package org.cloudburstmc.api.event.entity;

/**
 * The reason a hanging entity broke.
 */
public enum HangingBreakCause {
    ENTITY,
    EXPLOSION,
    OBSTRUCTION,
    PHYSICS,
    DEFAULT
}
