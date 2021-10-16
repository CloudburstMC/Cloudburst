package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;

@FunctionalInterface
public interface FloatBlockStateBehavior {

    float evaluate(BlockState state);
}
