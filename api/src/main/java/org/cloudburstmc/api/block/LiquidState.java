package org.cloudburstmc.api.block;

import org.cloudburstmc.api.registry.BlockRegistry;

/**
 * An immutable liquid state. The empty state represents the absence of liquid.
 * Read a block state's liquid with {@link BlockState#getLiquidState()}.
 */
public interface LiquidState {

    /**
     * Returns the empty liquid state.
     *
     * @return the empty liquid state
     * @throws IllegalStateException if no block registry implementation is available
     */
    static LiquidState empty() {
        return BlockRegistry.get().getEmptyLiquidState();
    }

    /**
     * @return whether this state contains no liquid
     */
    boolean isEmpty();

    /**
     * @return the liquid type
     */
    LiquidType getType();

    /**
     * @return whether this is a source state
     */
    boolean isSource();

    /**
     * @return whether this liquid is falling
     */
    boolean isFalling();

    /**
     * Checks whether another state belongs to the same liquid family.
     *
     * @param other the state to compare
     * @return whether both states are water, both are lava, or both are empty
     */
    boolean isSameFamily(LiquidState other);

    /**
     * @return the liquid amount from {@code 0} for empty to {@code 8} for full
     */
    int getAmount();

    /**
     * Returns the height represented by this state alone. Use
     * {@link org.cloudburstmc.api.level.Level#getLiquidHeight(org.cloudburstmc.math.vector.Vector3i)}
     * for the height at a position.
     *
     * @return the liquid height from {@code 0} to {@code 1}
     */
    float getOwnHeight();
}
