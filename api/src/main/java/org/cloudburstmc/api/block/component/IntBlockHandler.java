package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;

@FunctionalInterface
public interface IntBlockHandler {

    int execute(BlockState state);
}
