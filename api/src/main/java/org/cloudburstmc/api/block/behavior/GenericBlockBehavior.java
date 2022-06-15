package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface GenericBlockBehavior<T> {

    void execute(Behavior<GenericBlockBehavior.Executor<T>> behavior, Block block);

    @FunctionalInterface
    interface Executor<T> {
        void execute(Block block);
    }
}
