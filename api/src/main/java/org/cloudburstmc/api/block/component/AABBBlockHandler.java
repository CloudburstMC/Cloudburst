package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.AxisAlignedBB;

@FunctionalInterface
public interface AABBBlockHandler {

    AxisAlignedBB execute(BlockState state);
}
