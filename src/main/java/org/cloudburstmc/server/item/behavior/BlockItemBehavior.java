package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockItemBehavior extends CloudItemBehavior {

    private final BlockState blockState;

    public BlockItemBehavior(BlockState blockState) {
        this.blockState = blockState;
    }

    public BlockState getBlock(ItemStack item) {
        return blockState;
    }
}
