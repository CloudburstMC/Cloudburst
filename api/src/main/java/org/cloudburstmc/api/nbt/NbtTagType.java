package org.cloudburstmc.api.nbt;

/**
 * NBT tag types a property can be written as.
 *
 * @see NbtTag
 */
public enum NbtTagType {
    /**
     * Derive the type from the property's Java type.
     */
    AUTO,
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    STRING,
    BYTE_ARRAY,
    INT_ARRAY,
    LONG_ARRAY,
    LIST,
    COMPOUND
}
