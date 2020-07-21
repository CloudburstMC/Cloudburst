package org.cloudburstmc.server.block.behavior;

public class BlockBehaviorMushroomBrown extends BlockBehaviorMushroom {

    @Override
    public int getLightLevel() {
        return 1;
    }

    @Override
    protected int getType() {
        return 0;
    }
}
