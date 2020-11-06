package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BlockBehaviorOreRedstone extends BlockBehaviorSolid {

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            int count = new Random().nextInt(2) + 4;

            EnchantmentInstance fortune = hand.getEnchantment(EnchantmentTypes.FORTUNE);
            if (fortune != null && fortune.getLevel() >= 1) {
                count += new Random().nextInt(fortune.getLevel() + 1);
            }

            return new ItemStack[]{
                    ItemStack.get(ItemTypes.REDSTONE, count)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_TOUCH) { //type == Level.BLOCK_UPDATE_NORMAL ||
            block.set(BlockStates.REDSTONE_ORE.withTrait(BlockTraits.IS_EXTINGUISHED, false), false, false);

            return Level.BLOCK_UPDATE_WEAK;
        }

        return 0;
    }

    @Override
    public int getDropExp() {
        return ThreadLocalRandom.current().nextInt(1, 6);
    }


}
