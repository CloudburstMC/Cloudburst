package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;

@FunctionalInterface
public interface BooleanBlockStateHandler {

    boolean execute(BlockState state);
}
