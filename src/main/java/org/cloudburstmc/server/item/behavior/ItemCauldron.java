package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: CreeperFace
 * Nukkit Project
 */
public class ItemCauldron extends Item {

    public ItemCauldron(Identifier id) {
        super(id);
    }

    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockIds.CAULDRON);
    }
}