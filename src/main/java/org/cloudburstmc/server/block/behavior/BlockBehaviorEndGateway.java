package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStacks;
import org.cloudburstmc.api.util.data.BlockColor;

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
        return ItemStacks.AIR;
    }

}
