package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemCake extends Item {

    public ItemCake(Identifier id) {
        super(id);
    }

    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockIds.CAKE);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
