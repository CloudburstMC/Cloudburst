package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.block.BlockState;

public interface FlowerPot extends BlockEntity {

    BlockState getPlant();

    void setPlant(BlockState blockState);
}
