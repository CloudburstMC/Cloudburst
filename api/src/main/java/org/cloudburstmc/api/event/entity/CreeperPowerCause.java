package org.cloudburstmc.api.event.entity;

/**
 * Why a creeper's powered state changed.
 */
public enum CreeperPowerCause {
    /**
     * Lightning powered the creeper.
     */
    LIGHTNING,
    /**
     * Something other than lightning, such as a plugin, powered the creeper.
     */
    SET_ON,
    /**
     * The creeper's power was removed.
     */
    SET_OFF
}
