package org.cloudburstmc.api.block;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A read-only view of the block states stored at one level position.
 *
 * <p>A position always has a primary state. Its secondary state is air when
 * no block state coexists with the primary state.
 */
public interface BlockSnapshot {

    /**
     * Returns the primary state at this position.
     *
     * @return the primary block state
     */
    default BlockState getState() {
        return this.getState(BlockLayer.PRIMARY);
    }

    /**
     * Returns the state coexisting with the primary state.
     *
     * @return the secondary block state, or air when none is present
     */
    default BlockState getSecondaryState() {
        return this.getState(BlockLayer.SECONDARY);
    }

    /**
     * Returns the state stored in the requested layer.
     *
     * @param layer the block layer
     * @return the block state
     */
    BlockState getState(BlockLayer layer);

    /**
     * Finds the layer containing liquid.
     *
     * @return the liquid layer, or {@code null} when neither layer contains liquid
     */
    @Nullable
    default BlockLayer getLiquidLayer() {
        if (this.getState().getType().isLiquid()) {
            return BlockLayer.PRIMARY;
        }

        return this.getSecondaryState().getType().isLiquid() ? BlockLayer.SECONDARY : null;
    }

    /**
     * Returns the liquid state at this position.
     *
     * @return the liquid state, or the empty liquid state when none is present
     */
    default LiquidState getLiquid() {
        BlockLayer layer = this.getLiquidLayer();
        return layer == null ? LiquidState.empty() : LiquidState.of(this.getState(layer));
    }

    /**
     * Checks whether either block layer contains liquid.
     *
     * @return {@code true} when either block layer contains liquid
     */
    default boolean containsLiquid() {
        return this.getLiquidLayer() != null;
    }
}
