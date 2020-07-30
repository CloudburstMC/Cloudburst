package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.utils.Identifier;

public abstract class FloodableBlockBehavior extends BlockBehaviorTransparent {

    public FloodableBlockBehavior() {
        this(null);
    }

    public FloodableBlockBehavior(Identifier type) {
        super(type);
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
    public AxisAlignedBB getBoundingBox(Block block) {
        return null;
    }
}
