package org.cloudburstmc.server.block.behavior;

public abstract class BlockBehaviorPistonBase extends BlockBehaviorSolid {

    public boolean sticky;

    @Override
    public float getResistance() {
        return 2.5f;
    }


    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
