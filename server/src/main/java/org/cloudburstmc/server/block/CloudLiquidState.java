package org.cloudburstmc.server.block;

import lombok.EqualsAndHashCode;
import org.cloudburstmc.api.block.*;

import static java.util.Objects.requireNonNull;

/**
 * An immutable, nonempty liquid state derived from a block state's liquid depth.
 */
@EqualsAndHashCode
public class CloudLiquidState implements LiquidState {

    private final BlockState blockState;
    private final LiquidType type;
    private final int amount;
    private final boolean source;
    private final boolean falling;
    private final float ownHeight;

    public CloudLiquidState(BlockState blockState, LiquidType type) {
        this.blockState = requireNonNull(blockState, "blockState");
        this.type = requireNonNull(type, "type");
        int depth = blockState.ensureTrait(BlockTraits.LIQUID_DEPTH);
        this.source = depth == 0;
        this.falling = (depth & 8) != 0;
        this.amount = this.source || this.falling ? 8 : 8 - (depth & 7);
        this.ownHeight = this.amount / 9f;
    }

    /**
     * @param liquid the liquid to represent as a block state
     * @return the backing state, or air for an empty liquid
     * @throws IllegalArgumentException if a nonempty state is not owned by this server
     */
    public static BlockState toBlockState(LiquidState liquid) {
        if (requireNonNull(liquid, "liquid").isEmpty()) {
            return BlockStates.AIR;
        }

        if (!(liquid instanceof CloudLiquidState state)) {
            throw new IllegalArgumentException("Liquid state is not owned by this server: " + liquid);
        }

        return state.blockState;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public LiquidType getType() {
        return this.type;
    }

    @Override
    public boolean isSource() {
        return this.source;
    }

    @Override
    public boolean isFalling() {
        return this.falling;
    }

    @Override
    public boolean isSameFamily(LiquidState other) {
        requireNonNull(other, "other");
        if (other.isEmpty()) {
            return false;
        }

        return this.type.isSameFamily(other.getType());
    }

    @Override
    public int getAmount() {
        return this.amount;
    }

    @Override
    public float getOwnHeight() {
        return this.ownHeight;
    }

    @Override
    public String toString() {
        return this.blockState.toString();
    }
}
