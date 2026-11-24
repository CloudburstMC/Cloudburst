package org.cloudburstmc.api.event.entity;

/**
 * The reason a living entity is being spawned.
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
     * Hatched from an egg.
     */
    EGG,
    /**
     * Spawned by using a spawn egg.
     */
    SPAWNER_EGG,
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
     * Spawned by rehydrating a dried ghast.
     */
    REHYDRATION,
    /**
     * Spawned when a slime divides.
     */
    SLIME_SPLIT,
    /**
     * Spawned as reinforcement for another mob.
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
     * Spawned by infecting another mob.
     */
    INFECTION,
    /**
     * Spawned by curing another mob.
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
     * Spawned by an active potion effect.
     */
    POTION_EFFECT,
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
     * Spawned when a zombie converts into a drowned.
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
