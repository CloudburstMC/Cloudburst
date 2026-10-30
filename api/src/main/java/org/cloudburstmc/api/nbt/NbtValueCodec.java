package org.cloudburstmc.api.nbt;

/**
 * Converts a runtime value to its NBT form and back, for properties annotated with
 * {@link NbtCodec}. One runtime type can have several codecs, so each property picks the
 * encoding it is stored in.
 * <p>
 * A codec is created once per class through its no-arg constructor and shared, so it
 * must be stateless.
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
     * {@link org.cloudburstmc.nbt.NbtMap} or {@link org.cloudburstmc.nbt.NbtList}; {@code null}
     * leaves the key out
     */
    Object toNbt(V value);

    /**
     * Converts the NBT value stored under the property's key back.
     * <p>
     * The value is passed as stored, so a codec that accepts several numeric widths checks
     * for {@link Number}. When the value has the wrong kind or shape, the codec throws
     * {@link NbtKindMismatchException}: the property keeps its current value and the
     * mismatch is logged at warn level. Any other exception, a {@link ClassCastException}
     * included, fails the read.
     *
     * @param nbt the stored value, never {@code null}
     * @return the runtime value
     * @throws NbtKindMismatchException if the value has the wrong kind or shape
     */
    V fromNbt(Object nbt);
}
