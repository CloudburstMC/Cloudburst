package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorFenceNetherBrick extends BlockBehaviorFence {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public boolean canConnect(BlockState state) {
        return (state.getType() == BlockTypes.NETHER_BRICK_FENCE || state.inCategory(BlockCategory.FENCE_GATE)) || state.inCategory(BlockCategory.SOLID) && !state.inCategory(BlockCategory.TRANSPARENT);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.NETHERRACK_BLOCK_COLOR;
    }


}
