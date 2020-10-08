package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorClay extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(ItemTypes.CLAY_BALL, 4)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.CLAY_BLOCK_COLOR;
    }


}
