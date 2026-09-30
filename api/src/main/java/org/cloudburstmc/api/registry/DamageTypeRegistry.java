package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.entity.damage.DamageType;

/**
 * Provides registered damage definitions and accepts custom definitions while registration is open.
 */
public interface DamageTypeRegistry extends KeyedRegistry<DamageType> {

    /**
     * Registers a damage definition before registration closes.
     *
     * @param type the damage type to register
     * @throws IllegalArgumentException if its identifier is already registered
     * @throws IllegalStateException    if registration is closed
     */
    void register(DamageType type);
}
