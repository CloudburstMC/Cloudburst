package org.cloudburstmc.api.nbt.codec;

import lombok.NoArgsConstructor;
import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.api.util.data.DyeColor;

/**
 * Stores a dye colour as its wool data in a byte, as bed colours are stored today.
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
            throw new NbtKindMismatchException("Expected a number for wool colour, got "
                    + nbt.getClass().getSimpleName());
        }
        return DyeColor.getByWoolData(number.intValue());
    }
}
