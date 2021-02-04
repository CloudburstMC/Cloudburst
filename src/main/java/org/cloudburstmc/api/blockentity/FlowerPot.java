package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.server.block.BlockState;

public interface FlowerPot extends BlockEntity {

    BlockState getPlant();

    void setPlant(BlockState blockState);
}
