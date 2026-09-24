package org.cloudburstmc.api.event.entity;

/**
 * Describes why an active potion effect is changing.
 */
public enum PotionEffectCause {
    /**
     * Applied by an area effect cloud.
     */
    AREA_EFFECT_CLOUD,
    /**
     * Applied as part of an entity attack.
     */
    ATTACK,
    /**
     * Applied by a beacon.
     */
    BEACON,
    /**
     * Changed by a command.
     */
    COMMAND,
    /**
     * Removed because the entity died.
     */
    DEATH,
    /**
     * Removed because its duration elapsed.
     */
    EXPIRATION,
    /**
     * Changed by consuming food or drink.
     */
    FOOD,
    /**
     * Removed by drinking milk.
     */
    MILK,
    /**
     * Changed through the plugin API.
     */
    PLUGIN,
    /**
     * Applied by drinking a potion.
     */
    POTION_DRINK,
    /**
     * Applied by a splash potion.
     */
    POTION_SPLASH,
    /**
     * Applied by contact with a wither rose.
     */
    WITHER_ROSE,
    /**
     * Changed for an unspecified reason.
     */
    UNKNOWN
}
