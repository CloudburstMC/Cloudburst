package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface FloatBlockBehavior {

    float getProperty(Behavior behavior, BlockState state);

    @FunctionalInterface
    interface Executor {
        float execute(BlockState state);
    }
}
