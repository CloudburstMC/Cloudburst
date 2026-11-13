package org.cloudburstmc.api.persistence;

/**
 * Identifies a supported value type for a persistent data container.
 */
public final class PersistentDataType<T> {

    public static final PersistentDataType<Byte> BYTE = new PersistentDataType<>(Byte.class);
    public static final PersistentDataType<Short> SHORT = new PersistentDataType<>(Short.class);
    public static final PersistentDataType<Integer> INTEGER = new PersistentDataType<>(Integer.class);
    public static final PersistentDataType<Long> LONG = new PersistentDataType<>(Long.class);
    public static final PersistentDataType<Float> FLOAT = new PersistentDataType<>(Float.class);
    public static final PersistentDataType<Double> DOUBLE = new PersistentDataType<>(Double.class);
    public static final PersistentDataType<String> STRING = new PersistentDataType<>(String.class);
    public static final PersistentDataType<byte[]> BYTE_ARRAY = new PersistentDataType<>(byte[].class);
    public static final PersistentDataType<int[]> INTEGER_ARRAY = new PersistentDataType<>(int[].class);
    public static final PersistentDataType<long[]> LONG_ARRAY = new PersistentDataType<>(long[].class);

    private final Class<T> valueClass;

    private PersistentDataType(Class<T> valueClass) {
        this.valueClass = valueClass;
    }

    /**
     * Returns the Java class accepted by this type.
     *
     * @return the value class
     */
    public Class<T> getValueClass() {
        return this.valueClass;
    }
}
