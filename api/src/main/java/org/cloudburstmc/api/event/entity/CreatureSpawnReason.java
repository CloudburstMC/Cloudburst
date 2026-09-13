package org.cloudburstmc.api.event.entity;

/**
 * The reason a creature is being spawned.
 */
public enum CreatureSpawnReason {
    /**
     * Spawned by the level's natural spawning rules.
     */
    NATURAL,
    /**
     * Spawned as the passenger or vehicle of another entity.
     */
    JOCKEY,
    /**
     * Spawned by a mob spawner.
     */
    SPAWNER,
    /**
     * Spawned from an egg laid by another creature.
     */
    EGG,
    /**
     * Spawned by using a spawn egg.
     */
    SPAWN_EGG,
    /**
     * Spawned as a result of a lightning strike.
     */
    LIGHTNING,
    /**
     * Spawned from a constructed snow golem.
     */
    BUILD_SNOWMAN,
    /**
     * Spawned from a constructed iron golem.
     */
    BUILD_IRONGOLEM,
    /**
     * Spawned from a constructed wither.
     */
    BUILD_WITHER,
    /**
     * Spawned while defending a village.
     */
    VILLAGE_DEFENSE,
    /**
     * Spawned while attacking a village.
     */
    VILLAGE_INVASION,
    /**
     * Spawned through breeding.
     */
    BREEDING,
    /**
     * Spawned when a slime divides.
     */
    SLIME_SPLIT,
    /**
     * Spawned as reinforcement for another creature.
     */
    REINFORCEMENTS,
    /**
     * Spawned by a Nether portal.
     */
    NETHER_PORTAL,
    /**
     * Spawned from a spawn egg used by a dispenser.
     */
    DISPENSE_EGG,
    /**
     * Spawned by infecting another creature.
     */
    INFECTION,
    /**
     * Spawned by curing another creature.
     */
    CURED,
    /**
     * Spawned as an ocelot offspring.
     */
    OCELOT_BABY,
    /**
     * Spawned from an infested block.
     */
    SILVERFISH_BLOCK,
    /**
     * Spawned as a mount.
     */
    MOUNT,
    /**
     * Spawned as part of a trap.
     */
    TRAP,
    /**
     * Spawned after an ender pearl is used.
     */
    ENDER_PEARL,
    /**
     * Restored from a player's shoulder.
     */
    SHOULDER_ENTITY,
    /**
     * Spawned by converting a drowned creature.
     */
    DROWNED,
    /**
     * Spawned as a result of shearing.
     */
    SHEARED,
    /**
     * Spawned by a plugin.
     */
    CUSTOM,
    /**
     * Spawned without a more specific reason.
     */
    DEFAULT
}
