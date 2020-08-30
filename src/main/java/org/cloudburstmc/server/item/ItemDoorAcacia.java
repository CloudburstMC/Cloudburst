package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.Identifier;

public class ItemDoorAcacia extends Item {

    public ItemDoorAcacia(Identifier id) {
        super(id);
    }

    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockIds.ACACIA_DOOR);
    }
}
