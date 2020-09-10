package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.ItemStack;

public class EnchantmentBehavior {

    public int getWeight(EnchantmentInstance enchantment) {
        return enchantment.getType().getWeight();
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
        return enchantment.getType().getTarget().canEnchantItem(item);
    }

    public boolean isMajor() {
        return false;
    }
}
