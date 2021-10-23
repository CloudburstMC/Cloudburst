package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface IntBlockBehavior {

    int getProperty(Behavior behavior, BlockState state);

    @FunctionalInterface
    interface Executor {
        int execute(BlockState state);
    }
}
