package org.cloudburstmc.api.persistence;

/**
 * Converts between a supported storage type and a value used by a plugin.
 *
 * @param <P> storage type
 * @param <C> plugin value type
 */
public interface PersistentDataType<P, C> {

    PersistentDataType<Byte, Byte> BYTE = identity(Byte.class);
    PersistentDataType<Short, Short> SHORT = identity(Short.class);
    PersistentDataType<Integer, Integer> INTEGER = identity(Integer.class);
    PersistentDataType<Long, Long> LONG = identity(Long.class);
    PersistentDataType<Float, Float> FLOAT = identity(Float.class);
    PersistentDataType<Double, Double> DOUBLE = identity(Double.class);
    PersistentDataType<String, String> STRING = identity(String.class);
    PersistentDataType<byte[], byte[]> BYTE_ARRAY = identity(byte[].class);
    PersistentDataType<int[], int[]> INTEGER_ARRAY = identity(int[].class);
    PersistentDataType<long[], long[]> LONG_ARRAY = identity(long[].class);

    PersistentDataType<Byte, Boolean> BOOLEAN = new PersistentDataType<>() {
        @Override
        public Class<Byte> getPrimitiveType() {
            return Byte.class;
        }

        @Override
        public Byte toPrimitive(Boolean value) {
            return (byte) (value ? 1 : 0);
        }

        @Override
        public Boolean fromPrimitive(Byte value) {
            return value != 0;
        }
    };

    /**
     * Returns the class used to store values. It must match a storage class
     * used by one of the built-in adapters.
     *
     * @return the storage class
     */
    Class<P> getPrimitiveType();

    /**
     * Converts a plugin value for storage.
     *
     * @param value non-null plugin value
     * @return non-null storage value
     */
    P toPrimitive(C value);

    /**
     * Converts a stored value to its plugin representation.
     *
     * @param value non-null storage value
     * @return non-null plugin value
     */
    C fromPrimitive(P value);

    private static <T> PersistentDataType<T, T> identity(Class<T> type) {
        return new PersistentDataType<>() {
            @Override
            public Class<T> getPrimitiveType() {
                return type;
            }

            @Override
            public T toPrimitive(T value) {
                return value;
            }

            @Override
            public T fromPrimitive(T value) {
                return value;
            }
        };
    }
}
