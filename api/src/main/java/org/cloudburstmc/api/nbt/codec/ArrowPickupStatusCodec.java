package org.cloudburstmc.api.nbt.codec;

import org.cloudburstmc.api.entity.projectile.ArrowPickupStatus;
import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;

/**
 * Stores an arrow pickup rule as its byte value, as arrows are stored today.
 * Any other value reads as open pickup, exactly like the previous load path.
 */
public final class ArrowPickupStatusCodec implements NbtValueCodec<ArrowPickupStatus> {

    public ArrowPickupStatusCodec() {
    }

    @Override
    public Object toNbt(ArrowPickupStatus value) {
        return switch (value) {
            case DISALLOWED -> (byte) 0;
            case ALLOWED -> (byte) 1;
            case CREATIVE_ONLY -> (byte) 2;
        };
    }

    @Override
    public ArrowPickupStatus fromNbt(Object nbt) {
        if (!(nbt instanceof Number number)) {
            throw new NbtKindMismatchException("Expected a number for an arrow pickup rule, got "
                    + nbt.getClass().getSimpleName());
        }
        return switch (number.intValue()) {
            case 0 -> ArrowPickupStatus.DISALLOWED;
            case 2 -> ArrowPickupStatus.CREATIVE_ONLY;
            default -> ArrowPickupStatus.ALLOWED;
        };
    }
}
