package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;

public class BlockBehaviorWheat extends BlockBehaviorCrops {

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.WHEAT);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (block.getState().ensureTrait(BlockTraits.GROWTH) >= 0x07) {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.WHEAT),
                    ItemStack.get(ItemTypes.WHEAT, 0, (int) (4f * Math.random()))
            };
        } else {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.WHEAT)
            };
        }
    }
}
