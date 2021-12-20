package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface BooleanBlockBehavior {

    boolean test(Behavior<Executor> behavior, Block block);

    @FunctionalInterface
    interface Executor {
        boolean execute(Block block);
    }
}
