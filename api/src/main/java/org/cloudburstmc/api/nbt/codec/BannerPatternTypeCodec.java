package org.cloudburstmc.api.nbt.codec;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.api.util.data.BannerPattern;

/**
 * Stores a banner pattern shape by its name, as banner patterns are stored today.
 */
public final class BannerPatternTypeCodec implements NbtValueCodec<BannerPattern.Type> {

    public BannerPatternTypeCodec() {
    }

    @Override
    public Object toNbt(BannerPattern.Type value) {
        return value.getName();
    }

    @Override
    public BannerPattern.Type fromNbt(Object nbt) {
        if (!(nbt instanceof String name)) {
            throw new NbtKindMismatchException("Expected a string for a banner pattern, got "
                    + nbt.getClass().getSimpleName());
        }
        BannerPattern.Type type = BannerPattern.Type.getByName(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown banner pattern: " + name);
        }
        return type;
    }
}
