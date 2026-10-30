package org.cloudburstmc.api.nbt.codec;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;

/**
 * Stores a position as a compound of three ints, as ender-crystal beam targets
 * are stored today.
 */
public final class Vector3iCompoundCodec implements NbtValueCodec<Vector3i> {

    public Vector3iCompoundCodec() {
    }

    @Override
    public Object toNbt(Vector3i value) {
        return NbtMap.builder()
                .putInt("X", value.getX())
                .putInt("Y", value.getY())
                .putInt("Z", value.getZ())
                .build();
    }

    @Override
    public Vector3i fromNbt(Object nbt) {
        if (!(nbt instanceof NbtMap map)) {
            throw new NbtKindMismatchException("Expected a compound for a position, got "
                    + nbt.getClass().getSimpleName());
        }
        return Vector3i.from(map.getInt("X"), map.getInt("Y"), map.getInt("Z"));
    }
}
