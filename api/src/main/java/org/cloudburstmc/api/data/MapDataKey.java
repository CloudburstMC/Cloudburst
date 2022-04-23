package org.cloudburstmc.api.data;

import com.google.common.collect.ImmutableMap;
import org.cloudburstmc.api.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class MapDataKey<K, V> implements DataKey<Map<K, V>, Map<K, V>> {

    private final Identifier id;
    private final Class<K> keyType;
    private final Class<V> valueType;

    MapDataKey(Identifier id, Class<K> keyType, Class<V> valueType) {
        this.id = id;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public Class<Map<K, V>> getType() {
        return (Class) ImmutableMap.class;
    }

    @Override
    public Class<Map<K, V>> getMutableType() {
        return (Class) Map.class;
    }

    @Override
    public Function<Map<K, V>, Map<K, V>> getImmutableFunction() {
        return ImmutableMap::copyOf;
    }

    @Override
    public Function<Map<K, V>, Map<K, V>> getMutableFunction() {
        return HashMap::new;
    }
}
