package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.server.block.BlockState;

public interface FallingBlock extends Entity {

    BlockState getBlock();

    void setBlock(BlockState blockState);
}
