package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public final class EnchantmentDurability extends EnchantmentBehavior {

    @Override
    public boolean canEnchant(Enchantment enchantment, ItemStack item) {
        return (!item.isEmpty()
                && CloudItemRegistry.get().requireComponent(item.getType(), ItemComponents.DAMAGEABLE).get())
                || super.canEnchant(enchantment, item);
    }
}
