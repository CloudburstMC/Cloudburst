package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorIronBars extends BlockBehaviorThin {


    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().defaultState());
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{
                    this.toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.IRON_BLOCK_COLOR;
    }


}
