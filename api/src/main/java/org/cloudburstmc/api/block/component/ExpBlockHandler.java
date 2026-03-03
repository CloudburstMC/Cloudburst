package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;

import java.util.random.RandomGenerator;

@FunctionalInterface
public interface ExpBlockHandler {

    int execute(BlockState state, RandomGenerator random);
}
