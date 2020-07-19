package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
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
        return BlockState.get(BlockTypes.UNPOWERED_COMPARATOR);
    }
}