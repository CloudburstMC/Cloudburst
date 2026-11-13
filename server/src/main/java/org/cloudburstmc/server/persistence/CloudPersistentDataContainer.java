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

    private static final Set<Class<?>> STORAGE_TYPES = Set.of(
            Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            String.class, byte[].class, int[].class, long[].class
    );

    private final Runnable onChange;
    private NbtMap values = NbtMap.EMPTY;

    public CloudPersistentDataContainer(Runnable onChange) {
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    @Override
    public boolean has(Identifier key) {
        return this.values.containsKey(keyName(key));
    }

    @Override
    public boolean has(Identifier key, PersistentDataType<?, ?> type) {
        return storageType(type).isInstance(this.values.get(keyName(key)));
    }

    @Override
    public <P, C> Optional<C> get(Identifier key, PersistentDataType<P, C> type) {
        Object value = this.values.get(keyName(key));
        Class<P> storageType = storageType(type);
        return storageType.isInstance(value) ? Optional.of(type.fromPrimitive(storageType.cast(copy(value)))) : Optional.empty();
    }

    @Override
    public Set<Identifier> getKeys() {
        Set<Identifier> keys = new HashSet<>();

        for (String name : this.values.keySet()) {
            if (name.indexOf(':') <= 0) {
                continue;
            }

            try {
                Identifier key = Identifier.parse(name);
                if (isUsableKey(key) && name.equals(key.toString())) {
                    keys.add(key);
                }
            } catch (IllegalArgumentException ignored) {
                // Preserve malformed keys on disk without exposing them in the view.
            }
        }

        return Set.copyOf(keys);
    }

    @Override
    public <P, C> void set(Identifier key, PersistentDataType<P, C> type, C value) {
        String name = keyName(key);
        Class<P> storageType = storageType(type);
        P encoded = type.toPrimitive(Objects.requireNonNull(value, "value"));
        P checked = storageType.cast(Objects.requireNonNull(encoded, "encoded value"));

        if (!Objects.deepEquals(this.values.get(name), checked)) {
            NbtMapBuilder updated = this.values.toBuilder();
            updated.put(name, copy(checked));
            this.values = updated.build();
            this.onChange.run();
        }
    }

    @Override
    public void remove(Identifier key) {
        String name = keyName(key);
        if (this.values.containsKey(name)) {
            NbtMapBuilder updated = this.values.toBuilder();
            updated.remove(name);
            this.values = updated.build();
            this.onChange.run();
        }
    }

    public void load(NbtMap tag) {
        this.values = tag.getCompound(STORAGE_KEY, NbtMap.EMPTY);
    }

    public void save(NbtMapBuilder tag) {
        if (this.values.isEmpty()) {
            tag.remove(STORAGE_KEY);
        } else {
            tag.putCompound(STORAGE_KEY, this.values);
        }
    }

    private static String keyName(Identifier key) {
        if (!isUsableKey(Objects.requireNonNull(key, "key"))) {
            throw new IllegalArgumentException("Persistent data requires a non-empty namespace and path");
        }

        return key.toString();
    }

    private static boolean isUsableKey(Identifier key) {
        return !key.getNamespace().isEmpty() && !key.getName().isEmpty();
    }

    private static <P> Class<P> storageType(PersistentDataType<P, ?> type) {
        Class<P> storageType = Objects.requireNonNull(type, "type").getPrimitiveType();
        if (storageType == null || !STORAGE_TYPES.contains(storageType)) {
            throw new IllegalArgumentException("Unsupported persistent data storage type: " + storageType);
        }

        return storageType;
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
