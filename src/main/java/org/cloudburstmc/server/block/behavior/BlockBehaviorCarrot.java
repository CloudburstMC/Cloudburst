package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;

import java.util.Random;

public class BlockBehaviorCarrot extends BlockBehaviorCrops {

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (block.getState().ensureTrait(BlockTraits.GROWTH) >= 0x07) {
            return new ItemStack[]{
                    ItemStack.get(ItemIds.CARROT, 0, new Random().nextInt(3) + 1)
            };
        }
        return new ItemStack[]{
                ItemStack.get(ItemIds.CARROT)
        };
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemIds.CARROT);
    }
}
