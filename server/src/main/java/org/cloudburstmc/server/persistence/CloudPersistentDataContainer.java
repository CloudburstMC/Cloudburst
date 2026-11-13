package org.cloudburstmc.server.persistence;

import org.cloudburstmc.api.persistence.PersistentDataContainer;
import org.cloudburstmc.api.persistence.PersistentDataType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class CloudPersistentDataContainer implements PersistentDataContainer {

    public static final String STORAGE_KEY = "CloudburstPluginData";

    private final Runnable onChange;
    private NbtMap values = NbtMap.EMPTY;

    public CloudPersistentDataContainer(Runnable onChange) {
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    @Override
    public <T> void set(Identifier key, PersistentDataType<T> type, T value) {
        String name = Objects.requireNonNull(key, "key").toString();
        T checked = type.getValueClass().cast(Objects.requireNonNull(value, "value"));
        if (!Objects.deepEquals(this.values.get(name), checked)) {
            NbtMapBuilder updated = this.values.toBuilder();
            updated.put(name, copy(checked));
            this.values = updated.build();
            this.onChange.run();
        }
    }

    @Override
    public <T> Optional<T> get(Identifier key, PersistentDataType<T> type) {
        Object value = this.values.get(Objects.requireNonNull(key, "key").toString());
        Class<T> valueClass = type.getValueClass();
        return valueClass.isInstance(value) ? Optional.of(valueClass.cast(copy(value))) : Optional.empty();
    }

    @Override
    public void remove(Identifier key) {
        String name = Objects.requireNonNull(key, "key").toString();
        if (this.values.containsKey(name)) {
            NbtMapBuilder updated = this.values.toBuilder();
            updated.remove(name);
            this.values = updated.build();
            this.onChange.run();
        }
    }

    @Override
    public Set<Identifier> getKeys() {
        Set<Identifier> keys = new HashSet<>();
        for (String name : this.values.keySet()) {
            if (name.indexOf(':') >= 0) {
                keys.add(Identifier.parse(name));
            }
        }
        return Set.copyOf(keys);
    }

    public void load(NbtMap tag) {
        this.values = tag.getCompound(STORAGE_KEY, NbtMap.EMPTY);
    }

    public void save(NbtMapBuilder tag) {
        if (!this.values.isEmpty()) {
            tag.putCompound(STORAGE_KEY, this.values);
        }
    }

    private static Object copy(Object value) {
        return switch (value) {
            case byte[] array -> array.clone();
            case int[] array -> array.clone();
            case long[] array -> array.clone();
            default -> value;
        };
    }
}
