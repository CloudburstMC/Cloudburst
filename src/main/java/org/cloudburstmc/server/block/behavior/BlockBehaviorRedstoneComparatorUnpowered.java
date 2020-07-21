package org.cloudburstmc.server.block.behavior;

public class BlockBehaviorRedstoneComparatorUnpowered extends BlockBehaviorRedstoneComparator {

    @Override
    protected BlockBehaviorRedstoneComparator getUnpowered() {
        return this;
    }
}