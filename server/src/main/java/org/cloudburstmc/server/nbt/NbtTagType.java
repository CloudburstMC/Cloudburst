package org.cloudburstmc.server.nbt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.nbt.annotation.NbtTag;

/**
 * Selects the NBT representation of a property.
 *
 * @see NbtTag
 */
@Getter
@RequiredArgsConstructor
public enum NbtTagType {
    /**
     * Infers the tag type from the Java property type.
     */
    AUTO(NbtType.END),
    BYTE(NbtType.BYTE),
    SHORT(NbtType.SHORT),
    INT(NbtType.INT),
    LONG(NbtType.LONG),
    FLOAT(NbtType.FLOAT),
    DOUBLE(NbtType.DOUBLE),
    STRING(NbtType.STRING),
    BYTE_ARRAY(NbtType.BYTE_ARRAY),
    INT_ARRAY(NbtType.INT_ARRAY),
    LONG_ARRAY(NbtType.LONG_ARRAY),
    LIST(NbtType.LIST),
    COMPOUND(NbtType.COMPOUND);

    private final NbtType<?> nbtType;
}
