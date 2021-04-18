package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Smiteable;
//import org.cloudburstmc.api.entity.Smiteable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentDamageSmite extends EnchantmentDamage {

    public EnchantmentDamageSmite() {
        super(TYPE.SMITE);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 20;
    }

    @Override
    public float getDamageBonus(EnchantmentInstance enchantment, Entity entity) {
        if (entity instanceof Smiteable) {
            return enchantment.getLevel() * 2.5f;
        }

        return 0;
    }
}
