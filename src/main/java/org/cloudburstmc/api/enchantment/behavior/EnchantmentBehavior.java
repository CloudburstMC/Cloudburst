package org.cloudburstmc.api.enchantment.behavior;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentRarity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemStack;

public abstract class EnchantmentBehavior {

    public EnchantmentRarity getRarity(EnchantmentInstance enchantment) {
        return enchantment.getType().getRarity();
    }

    public int getWeight(EnchantmentInstance enchantment) {
        return this.getRarity(enchantment).getWeight();
    }

    public int getMaxLevel(EnchantmentInstance enchantment) {
        return enchantment.getType().getMaxLevel();
    }

    public int getMaxEnchantableLevel(EnchantmentInstance enchantment) {
        return getMaxLevel(enchantment);
    }

    public int getMinEnchantAbility(int level) {
        return 1 + level * 10;
    }

    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 5;
    }

    public float getProtectionFactor(EnchantmentInstance enchantment, EntityDamageEvent event) {
        return 0;
    }

    public float getDamageBonus(EnchantmentInstance enchantment, Entity entity) {
        return 0;
    }

    public void doPostAttack(EnchantmentInstance enchantment, Entity entity, Entity attacker) {

    }

    public void doPostHurt(EnchantmentInstance enchantment, Entity entity, Entity attacker) {

    }

    public boolean isCompatibleWith(EnchantmentInstance first, EnchantmentInstance enchantment) {
        return first != enchantment;
    }

    public boolean canEnchant(EnchantmentInstance enchantment, ItemStack item) {
        return enchantment.canEnchantItem(item);
    }

    public boolean isMajor() {
        return false;
    }

}
