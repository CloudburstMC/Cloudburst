package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;

public class EnchantmentBreach extends EnchantmentBehavior {

    @Override
    public float modifyArmorEffectiveness(Enchantment enchantment, float effectiveness) {
        return effectiveness - 0.15f * enchantment.level();
    }
}
