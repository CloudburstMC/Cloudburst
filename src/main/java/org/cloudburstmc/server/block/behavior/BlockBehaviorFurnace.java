package org.cloudburstmc.server.block.behavior;

public class BlockBehaviorFurnace extends BlockBehaviorFurnaceBurning {

    @Override
    public int getLightLevel() {
        return 0;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}