package org.cloudburstmc.api.block;

import lombok.EqualsAndHashCode;

import static java.util.Objects.requireNonNull;

/**
 * An immutable liquid state. The empty state represents the absence of liquid.
 */
@EqualsAndHashCode
public final class LiquidState {

    private static final LiquidState EMPTY = new LiquidState();

    private final BlockState blockState;
    private final LiquidType type;
    private final int amount;
    private final boolean source;
    private final boolean falling;
    private final float ownHeight;

    private LiquidState() {
        this.blockState = null;
        this.type = LiquidTypes.EMPTY;
        this.amount = 0;
        this.source = false;
        this.falling = false;
        this.ownHeight = 0;
    }

    LiquidState(BlockState blockState, LiquidType type) {
        this.blockState = blockState;
        this.type = type;
        int depth = blockState.ensureTrait(BlockTraits.LIQUID_DEPTH);
        this.source = depth == 0;
        this.falling = (depth & 8) != 0;
        this.amount = this.source || this.falling ? 8 : 8 - (depth & 7);
        this.ownHeight = this.amount / 9f;
    }

    /**
     * Returns the empty liquid state.
     *
     * @return the empty liquid state
     */
    public static LiquidState empty() {
        return EMPTY;
    }

    /**
     * Returns the liquid represented by a liquid block state.
     *
     * @param state the liquid block state, or air for the empty state
     * @return the corresponding liquid state
     * @throws IllegalArgumentException if the block state is neither liquid nor air
     */
    public static LiquidState of(BlockState state) {
        requireNonNull(state, "state");
        if (state.getType() == BlockTypes.AIR) {
            return EMPTY;
        }

        if (!state.getType().isLiquid()) {
            throw new IllegalArgumentException("Block state is not liquid: " + state);
        }

        return state.asLiquidState();
    }

    /**
     * @return whether this state contains no liquid
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * @return the liquid type
     */
    public LiquidType getType() {
        return this.type;
    }

    /**
     * @return whether this is a source state
     */
    public boolean isSource() {
        return this.source;
    }

    /**
     * @return whether this liquid is falling
     */
    public boolean isFalling() {
        return this.falling;
    }

    /**
     * Checks whether another state belongs to the same liquid family.
     *
     * @param other the state to compare
     * @return whether both states are water, both are lava, or both are empty
     */
    public boolean isSameFamily(LiquidState other) {
        requireNonNull(other, "other");
        if (this.isEmpty() || other.isEmpty()) {
            return this.isEmpty() && other.isEmpty();
        }

        return this.type.isSameFamily(other.type);
    }

    /**
     * @return the liquid amount from {@code 0} for empty to {@code 8} for full
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * Returns the height represented by this state alone. Use
     * {@link org.cloudburstmc.api.level.Level#getLiquidHeight(org.cloudburstmc.math.vector.Vector3i)}
     * for the height at a position.
     *
     * @return the liquid height from {@code 0} to {@code 1}
     */
    public float getOwnHeight() {
        return this.ownHeight;
    }

    @Override
    public String toString() {
        return isEmpty() ? "empty" : this.blockState.toString();
    }

    BlockState blockState() {
        return isEmpty() ? BlockStates.AIR : this.blockState;
    }
}
