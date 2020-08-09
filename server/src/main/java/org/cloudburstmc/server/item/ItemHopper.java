package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by CreeperFace on 13.5.2017.
 */
public class ItemHopper extends Item {

    public ItemHopper(Identifier id) {
        super(id);
    }

    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockTypes.HOPPER);
    }
}
