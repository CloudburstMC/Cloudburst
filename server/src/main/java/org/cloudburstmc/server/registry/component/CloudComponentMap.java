package org.cloudburstmc.server.registry.component;

import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.server.registry.CloudComponentRegistry;

import java.util.IdentityHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unchecked")
public class CloudComponentMap implements ComponentMap {

    private final Map<ComponentType<?>, Object> components = new IdentityHashMap<>();
    private final CloudComponentRegistry<?> registry;

    public CloudComponentMap(CloudComponentRegistry<?> registry) {
        this.registry = registry;
    }

    @Override
    public <H> H get(ComponentType<H> type) {
        return (H) components.get(type);
    }

    @Override
    public synchronized <H> ComponentMap set(ComponentType<H> type, H handler) {
        checkNotNull(type, "type");
        checkNotNull(handler, "handler");
        this.components.put(type, handler);
        return this;
    }

    public synchronized void bake() {
        this.registry.forEachComponent(this.components::putIfAbsent);
    }
}
