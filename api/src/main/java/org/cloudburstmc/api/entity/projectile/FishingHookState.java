package org.cloudburstmc.api.entity.projectile;

/**
 * The physical state of a fishing hook.
 */
public enum FishingHookState {
    /**
     * The hook is traveling through the air or resting against a block.
     */
    FLYING,
    /**
     * The hook is attached to an entity.
     */
    HOOKED_IN_ENTITY,
    /**
     * The hook is floating in water and may attract a catch.
     */
    BOBBING
}
