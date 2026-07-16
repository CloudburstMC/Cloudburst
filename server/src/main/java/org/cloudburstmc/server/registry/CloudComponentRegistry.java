package org.cloudburstmc.server.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.registry.ComponentRegistry;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.server.registry.component.CloudComponentMap;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

/**
 * Base implementation of {@link ComponentRegistry} for server-side block, item, and entity registries.
 * <p>
 * @param <T> the type managed by this registry
 */
@SuppressWarnings("unchecked")
public abstract class CloudComponentRegistry<T> implements ComponentRegistry<T> {

    /** Component defaults collected during registration. */
    private final Map<ComponentType<?>, Object> mutableComponents = new HashMap<>();
    /** Immutable component defaults published when registration closes. */
    private volatile Map<ComponentType<?>, Object> components = Map.of();
    private final Map<Identifier, ComponentType<?>> componentTypes = new HashMap<>();

    /**
     * Per-type component maps, keyed by type identity.
     */
    private final IdentityHashMap<T, CloudComponentMap> typeComponentMaps = new IdentityHashMap<>();
    private volatile boolean componentsFrozen;

    @Override
    public synchronized <H> void registerComponent(ComponentType<H> type) {
        checkNotNull(type, "type");
        checkState(!this.componentsFrozen, "Component registration is closed");
        ComponentType<?> existingType = this.componentTypes.get(type.getId());
        if (existingType != null) {
            throw new IllegalStateException("Component identifier " + type.getId()
                    + " is already registered for " + existingType.getType().getName());
        }

        this.componentTypes.put(type.getId(), type);
    }

    @Override
    public synchronized <H> void registerComponent(ComponentType<H> type, H defaultComponent) {
        checkNotNull(type, "type");
        checkNotNull(defaultComponent, "defaultComponent");
        checkArgument(type.getType().isInstance(defaultComponent),
                "Default for %s must implement %s", type.getId(), type.getType().getName());
        this.registerComponent(type);
        this.mutableComponents.put(type, defaultComponent);
    }

    @Override
    public <H> @Nullable H getDefaultComponent(ComponentType<H> type) {
        checkNotNull(type, "type");
        if (this.componentsFrozen) {
            return (H) this.components.get(type);
        }

        synchronized (this) {
            return (H) this.mutableComponents.get(type);
        }
    }

    public boolean isComponentRegistered(ComponentType<?> type) {
        return type.equals(this.componentTypes.get(type.getId()));
    }

    protected void putComponents(T type, CloudComponentMap map) {
        checkNotNull(type, "type");
        checkNotNull(map, "map");
        checkState(typeComponentMaps.putIfAbsent(type, map) == null, "%s is already registered", type);
    }

    protected CloudComponentMap getComponentMap(T type) {
        return typeComponentMaps.get(type);
    }

    protected synchronized void freezeComponentMaps() {
        checkState(!this.componentsFrozen, "Components are already frozen");
        this.typeComponentMaps.values().forEach(CloudComponentMap::freeze);
        this.components = Map.copyOf(this.mutableComponents);
        this.componentsFrozen = true;
    }

    @Override
    public @Nullable ComponentMap getComponents(T type) {
        return typeComponentMaps.get(checkNotNull(type, "type"));
    }
}
