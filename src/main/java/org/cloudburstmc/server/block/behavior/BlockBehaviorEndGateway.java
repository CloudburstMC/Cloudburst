package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorEndGateway extends BlockBehaviorSolid {
    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.BLACK_BLOCK_COLOR;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(BlockTypes.AIR);
    }

}
