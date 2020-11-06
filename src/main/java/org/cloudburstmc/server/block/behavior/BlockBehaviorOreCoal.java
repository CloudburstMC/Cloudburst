package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

public class BlockBehaviorOreCoal extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            int count = 1;
            EnchantmentInstance fortune = hand.getEnchantment(EnchantmentTypes.FORTUNE);
            if (fortune != null && fortune.getLevel() >= 1) {
                int i = ThreadLocalRandom.current().nextInt(fortune.getLevel() + 2) - 1;

                if (i < 0) {
                    i = 0;
                }

                count = i + 1;
            }

            return new ItemStack[]{
                    ItemStack.get(ItemTypes.COAL, count)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public int getDropExp() {
        return ThreadLocalRandom.current().nextInt(0, 2);
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.BLACK_BLOCK_COLOR;
    }
}
