package org.cloudburstmc.server.nbt.codec;

import lombok.NoArgsConstructor;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.nbt.NbtKindMismatchException;
import org.cloudburstmc.server.nbt.NbtValueCodec;

/**
 * Stores a dye color as its dye data in an int, as banner bases are stored today.
 */
@NoArgsConstructor
public class DyeDataIntCodec implements NbtValueCodec<DyeColor> {

    @Override
    public Object toNbt(DyeColor value) {
        return value.getDyeData();
    }

    @Override
    public DyeColor fromNbt(Object nbt) {
        if (!(nbt instanceof Number number)) {
            throw new NbtKindMismatchException("Expected a number for dye colour, got " + nbt.getClass().getSimpleName());
        }

        return DyeColor.getByDyeData(number.intValue());
    }
}
