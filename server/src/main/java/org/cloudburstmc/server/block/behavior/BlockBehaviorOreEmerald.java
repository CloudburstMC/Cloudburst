package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

import java.util.concurrent.ThreadLocalRandom;

public class BlockBehaviorOreEmerald extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 3;
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_IRON) {
            int count = 1;
            EnchantmentInstance fortune = hand.getEnchantment(CloudEnchantmentInstance.ID_FORTUNE_DIGGING);
            if (fortune != null && fortune.getLevel() >= 1) {
                int i = ThreadLocalRandom.current().nextInt(fortune.getLevel() + 2) - 1;

                if (i < 0) {
                    i = 0;
                }

                count = i + 1;
            }

            return new ItemStack[]{
                    ItemStack.get(ItemIds.EMERALD, 0, count)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public int getDropExp() {
        return ThreadLocalRandom.current().nextInt(3, 8);
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
