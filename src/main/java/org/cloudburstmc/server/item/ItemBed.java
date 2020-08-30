package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemBed extends Item {

    public ItemBed(Identifier id) {
        super(id);
    }

    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockIds.BED);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
