package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Level;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BlockBehaviorOreRedstone extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 3;
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_IRON) {
            int count = new Random().nextInt(2) + 4;

            EnchantmentInstance fortune = hand.getEnchantment(CloudEnchantmentInstance.ID_FORTUNE_DIGGING);
            if (fortune != null && fortune.getLevel() >= 1) {
                count += new Random().nextInt(fortune.getLevel() + 1);
            }

            return new ItemStack[]{
                    ItemStack.get(ItemIds.REDSTONE, 0, count)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_TOUCH) { //type == Level.BLOCK_UPDATE_NORMAL ||
            block.set(BlockState.get(BlockIds.LIT_REDSTONE_ORE), false, false);

            return Level.BLOCK_UPDATE_WEAK;
        }

        return 0;
    }

    @Override
    public int getDropExp() {
        return ThreadLocalRandom.current().nextInt(1, 6);
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
