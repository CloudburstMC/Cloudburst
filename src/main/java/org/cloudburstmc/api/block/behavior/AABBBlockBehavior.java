package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface AABBBlockBehavior {

    AxisAlignedBB getBoundingBox(Behavior behavior, BlockState state);

    @FunctionalInterface
    interface Executor {
        AxisAlignedBB execute(BlockState state);
    }
}
