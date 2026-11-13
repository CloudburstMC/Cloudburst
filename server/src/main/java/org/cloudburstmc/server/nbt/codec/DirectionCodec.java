package org.cloudburstmc.server.nbt.codec;

import lombok.NoArgsConstructor;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.nbt.NbtKindMismatchException;
import org.cloudburstmc.server.nbt.NbtValueCodec;

/**
 * Stores a direction as its 3-D index in a byte, as hanging entities are stored today.
 */
@NoArgsConstructor
public final class DirectionCodec implements NbtValueCodec<Direction> {

    @Override
    public Object toNbt(Direction value) {
        return (byte) value.getIndex();
    }

    @Override
    public Direction fromNbt(Object nbt) {
        if (!(nbt instanceof Number number)) {
            throw new NbtKindMismatchException("Expected a number for a direction, got " + nbt.getClass().getSimpleName());
        }

        return Direction.fromIndex(number.intValue());
    }
}
