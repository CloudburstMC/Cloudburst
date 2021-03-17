package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.item.ItemTypes;

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
                CloudItemRegistry.get().getItem(ItemTypes.MELON, Math.min(9, count))
        };
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.LIME_BLOCK_COLOR;
    }


}
