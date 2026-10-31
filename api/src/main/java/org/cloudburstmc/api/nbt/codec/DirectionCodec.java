package org.cloudburstmc.api.nbt.codec;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.api.util.Direction;

/**
 * Stores a direction as its 3-D index in a byte, as hanging entities are stored today.
 */
public final class DirectionCodec implements NbtValueCodec<Direction> {

    public DirectionCodec() {
    }

    @Override
    public Object toNbt(Direction value) {
        return (byte) value.getIndex();
    }

    @Override
    public Direction fromNbt(Object nbt) {
        if (!(nbt instanceof Number number)) {
            throw new NbtKindMismatchException("Expected a number for a direction, got "
                    + nbt.getClass().getSimpleName());
        }
        return Direction.fromIndex(number.intValue());
    }
}
