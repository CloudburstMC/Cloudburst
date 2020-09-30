package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.math.AxisAlignedBB;

public abstract class FloodableBlockBehavior extends BlockBehaviorTransparent {

    @Override
    public boolean canBeFlooded() {
        return true;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }


    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }
}
