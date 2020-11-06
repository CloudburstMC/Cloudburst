package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorRedSandstone extends BlockBehaviorSandstone {

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState());
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.ORANGE_BLOCK_COLOR;
    }
}
