package org.cloudburstmc.api.data;

import org.cloudburstmc.api.util.Identifier;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public sealed interface DataKey<T, M> permits ListDataKey, SimpleDataKey, MapDataKey {

    @SuppressWarnings("unchecked")
    static <T> SimpleDataKey<T> simple(Identifier id, Class<? super T> type) {
        checkNotNull(id, "id");
        checkNotNull(type, "type");
        return new SimpleDataKey<>(id, (Class<T>) type);
    }

    static <T> ListDataKey<T> list(Identifier id, Class<T> type) {
        checkNotNull(id, "id");
        checkNotNull(type, "type");
        return new ListDataKey<>(id, type);
    }

    static <K, V> MapDataKey<K, V> map(Identifier id, Class<K> keyType, Class<V> valueType) {
        checkNotNull(id, "id");
        checkNotNull(keyType, "keyType");
        checkNotNull(valueType, "valueType");
        return new MapDataKey<>(id, keyType, valueType);
    }

    Identifier getId();

    Class<T> getType();

    Class<M> getMutableType();

    Function<M, T> getImmutableFunction();

    Function<T, M> getMutableFunction();

    default T getDefaultValue() {
        return null;
    }
}
