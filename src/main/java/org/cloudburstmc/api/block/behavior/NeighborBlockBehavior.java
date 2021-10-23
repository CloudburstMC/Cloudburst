package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface NeighborBlockBehavior {

    void onNeighborChanged(Behavior<Executor> behavior, Block block, Block neighbor);

    @FunctionalInterface
    interface Executor {
        void executor(Block block, Block neighbor);
    }
}
