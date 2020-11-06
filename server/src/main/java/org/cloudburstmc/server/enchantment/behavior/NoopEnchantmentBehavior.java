package org.cloudburstmc.server.enchantment.behavior;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopEnchantmentBehavior extends EnchantmentBehavior {

    public static final NoopEnchantmentBehavior INSTANCE = new NoopEnchantmentBehavior();
}
