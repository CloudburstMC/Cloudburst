package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemTypes;

import java.util.Random;

public class BlockBehaviorPotato extends BlockBehaviorCrops {

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.POTATO);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (block.getState().ensureTrait(BlockTraits.GROWTH) == 7) {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.POTATO, new Random().nextInt(3) + 1)
            };
        } else {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.POTATO)
            };
        }
    }
}
