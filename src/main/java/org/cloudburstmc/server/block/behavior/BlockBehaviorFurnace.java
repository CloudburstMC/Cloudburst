package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;

public class BlockBehaviorFurnace extends BlockBehaviorFurnaceBurning {

    @Override
    public int getLightLevel(Block block) {
        return 0;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}