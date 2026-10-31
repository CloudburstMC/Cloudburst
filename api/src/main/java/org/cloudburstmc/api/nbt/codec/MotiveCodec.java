package org.cloudburstmc.api.nbt.codec;

import org.cloudburstmc.api.entity.misc.Painting;
import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;

/**
 * Stores a painting motive as its title string, as paintings are stored today.
 * An unknown title is a shape mismatch, so the property keeps its current value,
 * exactly like the previous load path, which ignored unknown motives.
 */
public final class MotiveCodec implements NbtValueCodec<Painting.Motive> {

    public MotiveCodec() {
    }

    @Override
    public Object toNbt(Painting.Motive value) {
        return value.getTitle();
    }

    @Override
    public Painting.Motive fromNbt(Object nbt) {
        if (!(nbt instanceof String name)) {
            throw new NbtKindMismatchException("Expected a string for a painting motive, got "
                    + nbt.getClass().getSimpleName());
        }
        Painting.Motive motive = Painting.Motive.from(name);
        if (motive == null) {
            throw new NbtKindMismatchException("Unknown painting motive: " + name);
        }
        return motive;
    }
}
