package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.server.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EnchantmentDamage extends EnchantmentBehavior {

    public enum TYPE {
        ALL,
        SMITE,
        ARTHROPODS
    }

    protected final TYPE damageType;

    protected EnchantmentDamage(TYPE type) {
        this.damageType = type;
    }

    @Override
    public boolean isCompatibleWith(EnchantmentInstance first, EnchantmentInstance enchantment) {
        return !(enchantment.getBehavior() instanceof EnchantmentDamage);
    }

    @Override
    public boolean canEnchant(EnchantmentInstance enchantment, ItemStack item) {
        return item.getBehavior().isAxe() || super.canEnchant(enchantment, item);
    }

    @Override
    public boolean isMajor() {
        return true;
    }
}
