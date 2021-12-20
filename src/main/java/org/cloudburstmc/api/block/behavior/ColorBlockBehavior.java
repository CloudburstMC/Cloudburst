package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.behavior.Behavior;
import org.cloudburstmc.api.util.data.BlockColor;

public interface ColorBlockBehavior {

    BlockColor execute(Behavior<Executor> behavior, Block block);

    @FunctionalInterface
    interface Executor {
        BlockColor execute(Block block);
    }
}
