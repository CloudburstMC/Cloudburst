package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface ComplexBlockBehavior {

    void execute(Behavior<Executor> behavior, Block block);

    @FunctionalInterface
    interface Executor {
        void execute(Block block);
    }
}
