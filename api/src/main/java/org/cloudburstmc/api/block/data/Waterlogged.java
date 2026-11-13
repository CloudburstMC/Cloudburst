package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockSnapshot;
import org.cloudburstmc.api.block.BlockState;

/**
 * A block snapshot whose primary block can contain water in its secondary layer.
 * Obtain this view through {@link BlockSnapshot#getData(Class)}.
 */
public interface Waterlogged extends BlockData {

    /**
     * @return the snapshot represented by this view
     */
    BlockSnapshot getSnapshot();

    @Override
    default BlockState getState() {
        return this.getSnapshot().getState();
    }

    /**
     * @return whether water occupies the secondary layer
     */
    boolean isWaterlogged();

    /**
     * Adds source water or removes existing water without changing a level.
     * Other block properties are retained unless they depend on water being present.
     *
     * @param waterlogged whether the resulting snapshot contains water
     * @return the resulting snapshot
     * @throws IllegalArgumentException if the resulting layers cannot coexist
     */
    BlockSnapshot withWaterlogged(boolean waterlogged);
}
