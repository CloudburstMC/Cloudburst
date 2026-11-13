package org.cloudburstmc.server.nbt.codec;

import lombok.NoArgsConstructor;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.nbt.NbtKindMismatchException;
import org.cloudburstmc.server.nbt.NbtValueCodec;

/**
 * Stores a shulker box facing as the byte of its 3-D index, as shulker boxes are stored today.
 */
@NoArgsConstructor
public class ShulkerFacingCodec implements NbtValueCodec<Direction> {

    @Override
    public Object toNbt(Direction value) {
        return (byte) value.getIndex();
    }

    @Override
    public Direction fromNbt(Object nbt) {
        if (!(nbt instanceof Number number)) {
            throw new NbtKindMismatchException("Expected a number for shulker facing, got " + nbt.getClass().getSimpleName());
        }

        return Direction.fromIndex(number.intValue());
    }
}
