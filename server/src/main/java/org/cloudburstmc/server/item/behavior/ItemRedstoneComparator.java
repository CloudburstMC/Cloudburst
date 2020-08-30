package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.Identifier;

/**
 * @author CreeperFace
 */
public class ItemRedstoneComparator extends Item {

    public ItemRedstoneComparator(Identifier id) {
        super(id);
    }

    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockIds.UNPOWERED_COMPARATOR);
    }
}