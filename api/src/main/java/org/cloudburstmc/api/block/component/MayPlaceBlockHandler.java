package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.Direction;

@FunctionalInterface
public interface MayPlaceBlockHandler {

    boolean execute(Block block, Direction direction);
}
