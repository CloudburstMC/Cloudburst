package org.cloudburstmc.api.entity;

/**
 * Identifies the action or effect responsible for healing.
 */
public enum RegainReason {
    /**
     * Regeneration in Peaceful difficulty.
     */
    REGEN,
    /**
     * Natural regeneration from sufficient food.
     */
    SATIATED,
    /**
     * An animal consuming food.
     */
    EATING,
    /**
     * Healing an ender dragon from an end crystal.
     */
    ENDER_CRYSTAL,
    /**
     * Instant healing from a potion or spell.
     */
    MAGIC,
    /**
     * Periodic healing from a potion or spell.
     */
    MAGIC_REGEN,
    /**
     * Healing during a wither's spawning sequence.
     */
    WITHER_SPAWN,
    /**
     * Healing from a wither effect.
     */
    WITHER,
    /**
     * Healing without a more specific cause.
     */
    CUSTOM
}
