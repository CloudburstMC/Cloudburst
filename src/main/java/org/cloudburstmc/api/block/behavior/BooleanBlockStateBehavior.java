package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface BooleanBlockStateBehavior {

    boolean test(Behavior<BooleanBlockStateBehavior.Executor> behavior, BlockState block);

    @FunctionalInterface
    interface Executor {
        boolean execute(BlockState block);
    }
}
