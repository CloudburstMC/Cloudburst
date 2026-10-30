package org.cloudburstmc.api.nbt.codec;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.api.util.Direction;

/**
 * Stores a shulker box facing as the byte of its 3-D index, as shulker boxes are stored today.
 */
public final class ShulkerFacingCodec implements NbtValueCodec<Direction> {

    public ShulkerFacingCodec() {
    }

    @Override
    public Object toNbt(Direction value) {
        return (byte) value.getIndex();
    }

    @Override
    public Direction fromNbt(Object nbt) {
        if (!(nbt instanceof Number number)) {
            throw new NbtKindMismatchException("Expected a number for shulker facing, got "
                    + nbt.getClass().getSimpleName());
        }
        return Direction.fromIndex(number.intValue());
    }
}
