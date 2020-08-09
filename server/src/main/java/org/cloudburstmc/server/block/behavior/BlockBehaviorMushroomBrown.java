package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;

public class BlockBehaviorMushroomBrown extends BlockBehaviorMushroom {

    @Override
    public int getLightLevel(Block block) {
        return 1;
    }

    @Override
    protected int getType() {
        return 0;
    }
}
