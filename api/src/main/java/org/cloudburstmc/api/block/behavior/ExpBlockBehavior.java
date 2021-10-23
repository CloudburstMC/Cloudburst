package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.behavior.Behavior;

import java.util.random.RandomGenerator;

public interface ExpBlockBehavior {

    int getExperience(Behavior behavior, BlockState state, RandomGenerator random);

    @FunctionalInterface
    interface Executor {
        int execute(BlockState state, RandomGenerator random);
    }
}
