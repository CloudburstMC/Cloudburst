package org.cloudburstmc.api.event.entity;

/**
 * Describes a change to an active potion effect.
 */
public enum PotionEffectAction {
    /**
     * An effect is being added.
     */
    ADDED,
    /**
     * An active effect is being replaced.
     */
    CHANGED,
    /**
     * An active effect is being removed.
     */
    REMOVED
}
