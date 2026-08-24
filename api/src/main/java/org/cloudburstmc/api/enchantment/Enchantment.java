package org.cloudburstmc.api.enchantment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable enchantment data stored on an item stack.
 *
 * @param type  the enchantment type
 * @param level the enchantment level, starting at one
 */
public record Enchantment(EnchantmentType type, int level) {

    public Enchantment {
        checkNotNull(type, "type");
        checkArgument(level > 0, "level must be positive");
    }
}
