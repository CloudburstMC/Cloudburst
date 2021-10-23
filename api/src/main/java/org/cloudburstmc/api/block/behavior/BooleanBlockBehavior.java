package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface BooleanBlockBehavior {

    boolean test(Behavior<Executor> behavior, BlockState state);

    @FunctionalInterface
    interface Executor {
        boolean execute(BlockState state);
    }
}
