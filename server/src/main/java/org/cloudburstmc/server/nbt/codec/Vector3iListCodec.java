package org.cloudburstmc.server.nbt.codec;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtType;

import java.util.List;

/**
 * Stores a position as a list of three ints, as end gateway exits are stored today.
 */
public final class Vector3iListCodec implements NbtValueCodec<Vector3i> {

    public Vector3iListCodec() {
    }

    @Override
    public Object toNbt(Vector3i value) {
        return new NbtList<>(NbtType.INT, List.of(value.getX(), value.getY(), value.getZ()));
    }

    @Override
    public Vector3i fromNbt(Object nbt) {
        if (!(nbt instanceof List<?> list) || list.size() < 3) {
            throw new NbtKindMismatchException("Expected a list of three ints for a position, got "
                    + describe(nbt));
        }
        Object x = list.get(0);
        Object y = list.get(1);
        Object z = list.get(2);
        if (!(x instanceof Number xNumber) || !(y instanceof Number yNumber) || !(z instanceof Number zNumber)) {
            throw new NbtKindMismatchException("Expected a list of three ints for a position");
        }
        return Vector3i.from(xNumber.intValue(), yNumber.intValue(), zNumber.intValue());
    }

    private static String describe(Object nbt) {
        if (nbt instanceof List<?> list) {
            return "a list of " + list.size() + " values";
        }
        return nbt.getClass().getSimpleName();
    }
}
