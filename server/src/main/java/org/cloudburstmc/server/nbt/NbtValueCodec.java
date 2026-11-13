package org.cloudburstmc.server.nbt;

import org.cloudburstmc.server.nbt.annotation.NbtCodec;

/**
 * Converts values for properties annotated with {@link NbtCodec}.
 * Codec instances are shared and must be thread-safe.
 *
 * @param <V> the runtime type
 */
public interface NbtValueCodec<V> {

    /**
     * Converts a value to NBT. The result is written exactly as returned.
     *
     * @param value the value, never {@code null}
     * @return a {@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float},
     * {@link Double}, {@link String}, {@code byte[]}, {@code int[]}, {@code long[]},
     * {@link org.cloudburstmc.nbt.NbtMap}, or {@link org.cloudburstmc.nbt.NbtList}.
     * Returning {@code null} omits the property.
     */
    Object toNbt(V value);

    /**
     * Decodes a tag without coercing its type. A kind mismatch preserves the existing
     * property value. Other decoding errors fail the read.
     *
     * @param nbt the stored value, never {@code null}
     * @return the runtime value
     * @throws NbtKindMismatchException if the value has the wrong kind or shape
     */
    V fromNbt(Object nbt);
}
