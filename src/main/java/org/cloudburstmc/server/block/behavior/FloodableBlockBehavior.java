package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class FloodableBlockBehavior extends BlockBehaviorTransparent {

    protected FloodableBlockBehavior(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeFlooded() {
        return true;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public float getHardness() {
        return 0;
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
    protected AxisAlignedBB recalculateBoundingBox() {
        return null;
    }
}
