package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.util.Identifier;

/**
 * Registry for effect types.
 */
public interface EffectRegistry extends KeyedRegistry<EffectType> {

    @Override
    default Identifier getId(EffectType value) {
        return value.getId();
    }
}
