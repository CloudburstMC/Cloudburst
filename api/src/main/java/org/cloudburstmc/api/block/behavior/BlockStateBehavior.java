package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;

@FunctionalInterface
public interface BlockStateBehavior<T> {

    T evaluate(BlockState value);
}
