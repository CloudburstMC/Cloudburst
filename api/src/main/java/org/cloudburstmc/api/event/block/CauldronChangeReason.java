package org.cloudburstmc.api.event.block;

/**
 * The cause of a cauldron's contents changing.
 */
public enum CauldronChangeReason {
    /**
     * A bucket being filled from the cauldron.
     */
    BUCKET_FILL,
    /**
     * A bucket being emptied into the cauldron.
     */
    BUCKET_EMPTY,
    /**
     * A bottle being filled from the cauldron.
     */
    BOTTLE_FILL,
    /**
     * A bottle being emptied into the cauldron.
     */
    BOTTLE_EMPTY,
    /**
     * A banner being washed.
     */
    BANNER_WASH,
    /**
     * Dyed armor being washed.
     */
    ARMOR_WASH,
    /**
     * Dye being added to water.
     */
    DYE_WATER,
    /**
     * Equipment being dyed.
     */
    ARMOR_DYE,
    /**
     * Arrows being tipped with a potion.
     */
    ARROW_TIP,
    /**
     * A shulker box being washed.
     */
    SHULKER_WASH,
    /**
     * A burning entity being extinguished.
     */
    EXTINGUISH,
    /**
     * Contents evaporating.
     */
    EVAPORATE,
    /**
     * Filling from rain, snow or a stalactite.
     */
    NATURAL_FILL,
    /**
     * A change without a more specific cause.
     */
    UNKNOWN
}
