package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTags;

/**
 * A flower pot and the plant it contains.
 */
public interface FlowerPot extends BlockEntity {

    /**
     * @return the planted block state or {@link BlockStates#AIR} if empty
     */
    BlockState getPlant();

    /**
     * Changes the plant without consuming or dropping items.
     *
     * @param blockState a plant in {@link BlockTags#POTTABLE_PLANTS} or {@link BlockStates#AIR} to empty the pot
     * @throws IllegalArgumentException if the block cannot be potted
     * @throws IllegalStateException    if the pot is no longer placed
     */
    void setPlant(BlockState blockState);
}
