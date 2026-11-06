package org.cloudburstmc.api.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.data.BlockData;
import org.cloudburstmc.api.util.data.BlockMirror;
import org.cloudburstmc.api.util.data.BlockRotation;

import java.util.Optional;

/**
 * A read-only view of the block states stored at one level position.
 *
 * <p>A position always has a primary state. Its secondary state is air when
 * no block state coexists with the primary state.
 */
public interface BlockSnapshot {

    /**
     * Returns a typed property view of the primary state. Views that depend on
     * both layers, such as {@link org.cloudburstmc.api.block.data.Waterlogged},
     * include the secondary state.
     *
     * @param dataType the requested view interface
     * @param <D>      the view type
     * @return the view, or empty if the snapshot does not support it
     */
    <D extends BlockData> Optional<D> getData(Class<D> dataType);

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
        return layer == null ? LiquidState.empty() : this.getState(layer).getLiquidState();
    }

    /**
     * Checks whether either block layer contains liquid.
     *
     * @return {@code true} when either block layer contains liquid
     */
    default boolean containsLiquid() {
        return this.getLiquidLayer() != null;
    }

    /**
     * Rotates both captured block layers without changing a level.
     *
     * @param rotation the horizontal rotation
     * @return the resulting snapshot
     * @throws IllegalArgumentException      if a layer cannot represent the transformed properties
     * @throws UnsupportedOperationException if a layer has an unsupported encoded orientation
     */
    BlockSnapshot rotate(BlockRotation rotation);

    /**
     * Reflects both captured block layers without changing a level.
     *
     * @param mirror the reflection
     * @return the resulting snapshot
     * @throws IllegalArgumentException      if a layer cannot represent the transformed properties
     * @throws UnsupportedOperationException if a layer has an unsupported encoded orientation
     */
    BlockSnapshot mirror(BlockMirror mirror);
}
