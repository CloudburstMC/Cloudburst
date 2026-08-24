package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentRarity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public abstract class EnchantmentBehavior {

    public EnchantmentRarity getRarity(Enchantment enchantment) {
        return enchantment.type().rarity();
    }

    public int getWeight(Enchantment enchantment) {
        return this.getRarity(enchantment).getWeight();
    }

    public int getMaxLevel(Enchantment enchantment) {
        return enchantment.type().maxLevel();
    }

    public int getMaxEnchantableLevel(Enchantment enchantment) {
        return getMaxLevel(enchantment);
    }

    public float getProtectionFactor(Enchantment enchantment, EntityDamageEvent event) {
        return 0;
    }

    public float getDamageBonus(Enchantment enchantment, Entity entity) {
        return 0;
    }

    public void doPostAttack(Enchantment enchantment, Entity entity, Entity attacker) {
    }

    public void doPostHurt(Enchantment enchantment, Entity entity, Entity attacker) {
    }

    public boolean canEnchant(Enchantment enchantment, ItemStack item) {
        ItemType itemType = item.getType();
        return itemType != null
                && CloudItemRegistry.get().requireComponent(itemType, ItemComponents.CAN_ENCHANT_WITH)
                .execute(item, enchantment.type());
    }

    public boolean isMajor() {
        return false;
    }
}
