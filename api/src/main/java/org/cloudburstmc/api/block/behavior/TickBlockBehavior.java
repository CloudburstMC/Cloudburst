package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.behavior.Behavior;

import java.util.random.RandomGenerator;

public interface TickBlockBehavior {

    void onTick(Behavior<Executor> behavior, Block block, RandomGenerator random);

    @FunctionalInterface
    interface Executor {
        void execute(Block block, RandomGenerator random);
    }
}
