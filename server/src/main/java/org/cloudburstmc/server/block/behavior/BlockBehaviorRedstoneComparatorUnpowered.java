package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * @author CreeperFace
 */
public class BlockBehaviorRedstoneComparatorUnpowered extends BlockBehaviorRedstoneComparator {

    public BlockBehaviorRedstoneComparatorUnpowered(Identifier id) {
        super(id);
    }

    @Override
    protected BlockBehaviorRedstoneComparator getUnpowered() {
        return this;
    }
}