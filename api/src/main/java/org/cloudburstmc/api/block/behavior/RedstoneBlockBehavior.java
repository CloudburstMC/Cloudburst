package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface RedstoneBlockBehavior {

    int onRedstoneUpdate(Behavior behavior, Block block);

    @FunctionalInterface
    interface Executor {
        int execute(Block block);
    }
}
