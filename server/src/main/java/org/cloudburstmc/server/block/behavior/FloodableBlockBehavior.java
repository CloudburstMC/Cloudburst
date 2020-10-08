package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.math.AxisAlignedBB;

public abstract class FloodableBlockBehavior extends BlockBehaviorTransparent {

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }
}
