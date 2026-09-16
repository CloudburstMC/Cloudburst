package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public abstract class EnchantmentBehavior {

    public float getDamageProtection(Enchantment enchantment, EntityDamageEvent event) {
        return 0;
    }

    public float modifyDamage(Enchantment enchantment, Entity target, float damage) {
        return damage;
    }

    public float modifyKnockback(Enchantment enchantment, Entity target, float knockback) {
        return knockback;
    }

    public void onPostAttack(Enchantment enchantment, Entity attacker, Entity target) {
    }

    public ItemStack onPostHurt(Enchantment enchantment, ItemStack item, Entity wearer, Entity attacker) {
        return item;
    }

    public boolean canEnchant(Enchantment enchantment, ItemStack item) {
        ItemType itemType = item.getType();
        return CloudItemRegistry.get().requireComponent(itemType, ItemComponents.CAN_ENCHANT_WITH).execute(item, enchantment.type());
    }
}
