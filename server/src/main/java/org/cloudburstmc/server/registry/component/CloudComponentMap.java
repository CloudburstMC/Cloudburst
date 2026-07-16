package org.cloudburstmc.server.registry.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.server.registry.CloudComponentRegistry;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unchecked")
public final class CloudComponentMap implements ComponentBuilder {

    private final Map<ComponentType<?>, Object> mutableComponents = new HashMap<>();
    private volatile Map<ComponentType<?>, Object> components = Map.of();
    private final CloudComponentRegistry<?> registry;
    private volatile boolean frozen;

    public CloudComponentMap(CloudComponentRegistry<?> registry) {
        this.registry = registry;
    }

    @Override
    public <H> @Nullable H get(ComponentType<H> type) {
        checkNotNull(type, "type");
        H component;
        if (this.frozen) {
            component = (H) this.components.get(type);
        } else {
            synchronized (this) {
                component = (H) this.mutableComponents.get(type);
            }
        }

        return component != null ? component : this.registry.getDefaultComponent(type);
    }

    @Override
    public synchronized <H> ComponentBuilder set(ComponentType<H> type, H handler) {
        checkNotNull(type, "type");
        checkNotNull(handler, "handler");
        if (this.frozen) {
            throw new IllegalStateException("Components have been frozen");
        }

        if (!this.registry.isComponentRegistered(type)) {
            throw new IllegalArgumentException("Component is not registered: " + type.getId());
        }

        if (!type.getType().isInstance(handler)) {
            throw new IllegalArgumentException("Component " + type.getId() + " must implement " + type.getType().getName());
        }

        this.mutableComponents.put(type, handler);
        return this;
    }

    public synchronized void freeze() {
        if (this.frozen) {
            throw new IllegalStateException("Components have already been frozen");
        }

        this.components = Map.copyOf(this.mutableComponents);
        this.frozen = true;
    }
}
