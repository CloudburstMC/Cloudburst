package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.util.Identifier;

import java.util.Optional;

/**
 * Registry for enchantment types.
 */
public interface EnchantmentRegistry extends KeyedRegistry<EnchantmentType> {

    /**
     * Finds an enchantment type by identifier.
     *
     * @param id enchantment identifier
     * @return matching enchantment type, if registered
     */
    @Override
    Optional<EnchantmentType> get(Identifier id);

    /**
     * Returns the identifier for a registered enchantment type.
     *
     * @param value registered enchantment type
     * @return enchantment identifier
     */
    @Override
    default Identifier getId(EnchantmentType value) {
        return value.identifier();
    }
}
