package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface DescriptionBlockBehavior {

    String getDescription(Behavior behavior, BlockState state);

    @FunctionalInterface
    interface Executor {
        String execute(BlockState state);
    }
}
