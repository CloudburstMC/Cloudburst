package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.CloudLevel;

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
                    CloudItemRegistry.get().getItem(ItemTypes.REDSTONE, count)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_TOUCH) { //type == Level.BLOCK_UPDATE_NORMAL ||
            block.set(BlockStates.REDSTONE_ORE.withTrait(BlockTraits.IS_EXTINGUISHED, false), false, false);

            return CloudLevel.BLOCK_UPDATE_WEAK;
        }

        return 0;
    }

    @Override
    public int getDropExp() {
        return ThreadLocalRandom.current().nextInt(1, 6);
    }


}
