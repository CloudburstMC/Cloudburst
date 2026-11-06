package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.block.LiquidType;
import org.cloudburstmc.api.block.LiquidTypes;

import static java.util.Objects.requireNonNull;

/**
 * The canonical state for the absence of liquid.
 */
public enum EmptyLiquidState implements LiquidState {
    INSTANCE;

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public LiquidType getType() {
        return LiquidTypes.EMPTY;
    }

    @Override
    public boolean isSource() {
        return false;
    }

    @Override
    public boolean isFalling() {
        return false;
    }

    @Override
    public boolean isSameFamily(LiquidState other) {
        return requireNonNull(other, "other").isEmpty();
    }

    @Override
    public int getAmount() {
        return 0;
    }

    @Override
    public float getOwnHeight() {
        return 0;
    }

    @Override
    public String toString() {
        return "empty";
    }
}
