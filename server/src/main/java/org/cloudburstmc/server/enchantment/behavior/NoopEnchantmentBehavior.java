package org.cloudburstmc.server.enchantment.behavior;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopEnchantmentBehavior extends EnchantmentBehavior {

    public static final NoopEnchantmentBehavior INSTANCE = new NoopEnchantmentBehavior();
}
