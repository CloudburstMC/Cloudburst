package org.cloudburstmc.api.event.entity;

/**
 * The reason an entity was removed from its level.
 */
public enum EntityRemoveCause {
    /**
     * The entity died.
     */
    DEATH,
    /**
     * The entity expired or despawned naturally.
     */
    DESPAWN,
    /**
     * The entity was removed to drop an item. The item may not spawn.
     */
    DROP,
    /**
     * The entity entered or became part of a block.
     */
    ENTER_BLOCK,
    /**
     * The entity was removed as part of an explosion.
     */
    EXPLODE,
    /**
     * The entity was removed after hitting a block or entity.
     */
    HIT,
    /**
     * The entity merged into another entity.
     */
    MERGE,
    /**
     * The entity was removed after leaving the world's bounds.
     */
    OUT_OF_WORLD,
    /**
     * The entity was collected.
     */
    PICKUP,
    /**
     * The entity was removed because a player disconnected.
     */
    PLAYER_QUIT,
    /**
     * The entity was removed through the plugin API.
     */
    PLUGIN,
    /**
     * The entity transformed into a different entity.
     */
    TRANSFORMATION,
    /**
     * The entity's chunk unloaded.
     */
    UNLOAD,
    /**
     * The entity was discarded without a more specific cause.
     */
    DISCARD
}
