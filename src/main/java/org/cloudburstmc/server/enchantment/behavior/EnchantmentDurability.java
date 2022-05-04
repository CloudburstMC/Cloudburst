package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Random;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentDurability extends EnchantmentBehavior {

    @Override
    public int getMinEnchantAbility(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 50;
    }

    @Override
    public boolean isCompatibleWith(EnchantmentInstance first, EnchantmentInstance enchantment) {
        return super.isCompatibleWith(first, enchantment) && enchantment.getType() != EnchantmentTypes.FORTUNE;
    }

    @Override
    public boolean canEnchant(EnchantmentInstance enchantment, ItemStack item) {
        return CloudItemRegistry.get().getBehavior(item.getType(), ItemBehaviors.GET_MAX_DURABILITY).execute() >= 0 || super.canEnchant(enchantment, item);
    }

//    TODO Method isn't used?
//    public static boolean negateDamage(ItemStack item, int level, Random random) {
//        return !(item.getBehavior().isArmor() && random.nextFloat() < 0.6f) && random.nextInt(level + 1) > 0;
//    }
}
