package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * @author CreeperFace
 */
public class BlockBehaviorRedstoneComparatorPowered extends BlockBehaviorRedstoneComparator {

    public BlockBehaviorRedstoneComparatorPowered(Identifier id) {
        super(id);
        this.isPowered = true;
    }

    @Override
    protected BlockBehaviorRedstoneComparator getPowered() {
        return this;
    }
}
