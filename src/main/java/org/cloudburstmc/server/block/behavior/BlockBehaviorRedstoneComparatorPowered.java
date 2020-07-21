package org.cloudburstmc.server.block.behavior;

public class BlockBehaviorRedstoneComparatorPowered extends BlockBehaviorRedstoneComparator {

    public BlockBehaviorRedstoneComparatorPowered() {
        this.isPowered = true;
    }

    @Override
    protected BlockBehaviorRedstoneComparator getPowered() {
        return this;
    }
}
