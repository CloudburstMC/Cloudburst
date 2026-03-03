package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

@FunctionalInterface
public interface ColorBlockHandler {

    BlockColor execute(Block block);
}
