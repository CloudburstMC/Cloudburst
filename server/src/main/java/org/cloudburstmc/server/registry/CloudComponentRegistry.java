package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.registry.ComponentRegistry;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.server.registry.component.CloudComponentMap;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base implementation of {@link ComponentRegistry} for server-side block, item, and entity registries.
 * <p>
 * Stores both global component defaults (applied to every type at bake time) and per-type
 * {@link CloudComponentMap}s in a single shared {@link IdentityHashMap}.
 *
 * @param <T> the type managed by this registry
 */
@SuppressWarnings("unchecked")
public abstract class CloudComponentRegistry<T> implements ComponentRegistry<T> {

    /**
     * Global component defaults; copied into every per-type map during {@link CloudComponentMap#bake()}.
     */
    protected final Map<ComponentType<?>, Object> components = new HashMap<>();

    /**
     * Per-type component maps, keyed by type identity.
     */
    private final IdentityHashMap<T, CloudComponentMap> typeComponentMaps = new IdentityHashMap<>();

    @Override
    public <H> void registerComponent(ComponentType<H> type, H defaultComponent) {
        checkNotNull(type, "type");
        checkNotNull(defaultComponent, "defaultComponent");
        checkState(!this.components.containsKey(type), "%s has already been registered", type.getId());

        this.components.put(type, defaultComponent);
    }

    @Override
    public <H> H getDefaultComponent(ComponentType<H> type) {
        return (H) components.get(type);
    }

    public boolean isComponentRegistered(ComponentType<?> type) {
        return components.containsKey(type);
    }

    public void forEachComponent(BiConsumer<ComponentType<?>, Object> consumer) {
        checkNotNull(consumer, "consumer");
        for (Map.Entry<ComponentType<?>, Object> entry : this.components.entrySet()) {
            consumer.accept(entry.getKey(), entry.getValue());
        }
    }

    protected void putComponents(T type, CloudComponentMap map) {
        checkNotNull(type, "type");
        checkNotNull(map, "map");
        checkState(typeComponentMaps.putIfAbsent(type, map) == null, "%s is already registered", type);
    }

    protected CloudComponentMap getComponentMap(T type) {
        return typeComponentMaps.get(type);
    }

    protected CloudComponentMap newUnregisteredComponentMap() {
        CloudComponentMap map = new CloudComponentMap(this);
        map.bake();
        return map;
    }

    @Override
    public ComponentMap getComponents(T type) {
        return typeComponentMaps.get(type);
    }
}
