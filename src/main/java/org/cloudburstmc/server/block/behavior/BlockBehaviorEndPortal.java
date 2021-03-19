package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStacks;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorEndPortal extends FloodableBlockBehavior {
    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.BLACK_BLOCK_COLOR;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }



    @Override
    public ItemStack toItem(Block block) {
        return ItemStacks.AIR;
    }
}
