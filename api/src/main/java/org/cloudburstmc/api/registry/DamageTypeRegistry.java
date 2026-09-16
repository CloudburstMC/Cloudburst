package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.util.Identifier;

/**
 * Registry of damage types.
 */
public interface DamageTypeRegistry extends KeyedRegistry<DamageType> {

    @Override
    default Identifier getId(DamageType value) {
        return value.getId();
    }
}
