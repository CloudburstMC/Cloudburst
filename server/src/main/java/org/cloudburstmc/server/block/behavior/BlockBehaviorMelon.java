package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.Random;

public class BlockBehaviorMelon extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        Random random = new Random();
        int count = 3 + random.nextInt(5);

        EnchantmentInstance fortune = hand.getEnchantment(EnchantmentTypes.FORTUNE);
        if (fortune != null && fortune.getLevel() >= 1) {
            count += random.nextInt(fortune.getLevel() + 1);
        }

        return new ItemStack[]{
                ItemStack.get(ItemTypes.MELON, Math.min(9, count))
        };
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.LIME_BLOCK_COLOR;
    }


}
