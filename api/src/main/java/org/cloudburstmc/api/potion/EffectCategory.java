package org.cloudburstmc.api.potion;

/**
 * Describes how an effect is classified.
 */
public enum EffectCategory {
    /**
     * Helps the affected entity.
     */
    BENEFICIAL,
    /**
     * Harms or hinders the affected entity.
     */
    HARMFUL,
    /**
     * Has no generally beneficial or harmful classification.
     */
    NEUTRAL
}
