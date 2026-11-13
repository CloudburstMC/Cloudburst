package org.cloudburstmc.server.nbt.codec;

import lombok.NoArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.nbt.NbtKindMismatchException;
import org.cloudburstmc.server.nbt.NbtValueCodec;

/**
 * Stores a position as a compound of three ints, as ender-crystal beam targets
 * are stored today.
 */
@NoArgsConstructor
public final class Vector3iCompoundCodec implements NbtValueCodec<Vector3i> {

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
            throw new NbtKindMismatchException("Expected a compound for a position, got " + nbt.getClass().getSimpleName());
        }

        return Vector3i.from(map.getInt("X"), map.getInt("Y"), map.getInt("Z"));
    }
}
