package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.Identifier;

public class ItemDoorBirch extends Item {

    public ItemDoorBirch(Identifier id) {
        super(id);
    }


    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockIds.BIRCH_DOOR);
    }
}
