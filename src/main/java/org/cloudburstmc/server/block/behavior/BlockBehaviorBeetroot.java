package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;

import static org.cloudburstmc.server.item.ItemTypes.BEETROOT;
import static org.cloudburstmc.server.item.ItemTypes.BEETROOT_SEEDS;

public class BlockBehaviorBeetroot extends BlockBehaviorCrops {

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(BEETROOT_SEEDS);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (block.getState().ensureTrait(BlockTraits.GROWTH) >= 0x07) {
            return new ItemStack[]{
                    ItemStack.get(BEETROOT),
                    ItemStack.get(BEETROOT_SEEDS, (int) (4f * Math.random()))
            };
        } else {
            return new ItemStack[]{
                    ItemStack.get(BEETROOT_SEEDS)
            };
        }
    }
}
