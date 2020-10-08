package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockNetherWartBlockBehavior extends BlockBehaviorSolid {

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                toItem(block)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.RED_BLOCK_COLOR;
    }
}
