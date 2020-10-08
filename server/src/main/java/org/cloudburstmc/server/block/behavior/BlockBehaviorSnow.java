package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorSnow extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.SNOWBALL, 4)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.SNOW_BLOCK_COLOR;
    }


}
