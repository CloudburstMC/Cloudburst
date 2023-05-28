package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface EntityBlockBehavior {

    void execute(Behavior<Executor> behavior, Block block, Entity entity);

    @FunctionalInterface
    interface Executor {
        void execute(Block block, Entity entity);
    }
}
