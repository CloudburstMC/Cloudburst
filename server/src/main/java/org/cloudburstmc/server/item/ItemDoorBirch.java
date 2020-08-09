package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.utils.Identifier;

public class ItemDoorBirch extends Item {

    public ItemDoorBirch(Identifier id) {
        super(id);
    }


    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockTypes.BIRCH_DOOR);
    }
}
