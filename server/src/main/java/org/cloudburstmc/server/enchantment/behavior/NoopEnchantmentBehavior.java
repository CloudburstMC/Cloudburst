package org.cloudburstmc.server.enchantment.behavior;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoopEnchantmentBehavior extends EnchantmentBehavior {

    public static final NoopEnchantmentBehavior INSTANCE = new NoopEnchantmentBehavior();
}
