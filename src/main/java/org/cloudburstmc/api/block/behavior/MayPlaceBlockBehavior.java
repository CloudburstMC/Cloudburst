package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface MayPlaceBlockBehavior {

    boolean mayPlace(Behavior behavior, Block block, Direction direction);

    @FunctionalInterface
    interface Executor {
        boolean execute(Block block, Direction direction);
    }
}
