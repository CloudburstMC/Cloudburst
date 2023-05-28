package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;

public interface FallingBlock extends Entity {

    BlockState getBlock();

    void setBlock(BlockState blockState);
}
