package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;

@FunctionalInterface
public interface DescriptionBlockHandler {

    String execute(BlockState state);
}
