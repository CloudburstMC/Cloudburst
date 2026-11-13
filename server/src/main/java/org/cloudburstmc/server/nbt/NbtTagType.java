package org.cloudburstmc.server.nbt;

/**
 * NBT tag types a property can be written as.
 *
 * @see NbtTag
 */
public enum NbtTagType {
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
