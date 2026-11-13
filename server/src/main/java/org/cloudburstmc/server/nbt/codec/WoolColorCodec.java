package org.cloudburstmc.server.nbt.codec;

import lombok.NoArgsConstructor;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.nbt.NbtKindMismatchException;
import org.cloudburstmc.server.nbt.NbtValueCodec;

/**
 * Stores a dye color as its wool data in a byte, as bed colors are stored today.
 */
@NoArgsConstructor
public final class WoolColorCodec implements NbtValueCodec<DyeColor> {

    @Override
    public Object toNbt(DyeColor value) {
        return (byte) value.getWoolData();
    }

    @Override
    public DyeColor fromNbt(Object nbt) {
        if (!(nbt instanceof Number number)) {
            throw new NbtKindMismatchException("Expected a number for wool colour, got " + nbt.getClass().getSimpleName());
        }

        return DyeColor.getByWoolData(number.intValue());
    }
}
