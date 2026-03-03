package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

@FunctionalInterface
public interface BooleanBlockHandler {

    boolean execute(Block block);
}
