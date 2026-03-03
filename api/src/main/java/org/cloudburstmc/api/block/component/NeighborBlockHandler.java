package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

@FunctionalInterface
public interface NeighborBlockHandler {

    void execute(Block block, Block neighbor);
}
