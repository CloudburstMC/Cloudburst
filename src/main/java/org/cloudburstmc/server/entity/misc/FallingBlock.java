package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;

public interface FallingBlock extends Entity {

    BlockState getBlock();

    void setBlock(BlockState blockState);
}
