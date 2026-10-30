package org.cloudburstmc.api.nbt.codec;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.api.util.data.DyeColor;

/**
 * Stores a dye colour as its dye data in an int, as banner bases are stored today.
 */
public final class DyeDataIntCodec implements NbtValueCodec<DyeColor> {

    public DyeDataIntCodec() {
    }

    @Override
    public Object toNbt(DyeColor value) {
        return value.getDyeData();
    }

    @Override
    public DyeColor fromNbt(Object nbt) {
        if (!(nbt instanceof Number number)) {
            throw new NbtKindMismatchException("Expected a number for dye colour, got "
                    + nbt.getClass().getSimpleName());
        }
        return DyeColor.getByDyeData(number.intValue());
    }
}
