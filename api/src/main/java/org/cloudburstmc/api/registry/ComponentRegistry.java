package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.util.component.ComponentMap;

/**
 * A registry that stores per-type {@link ComponentMap}s and global component defaults.
 *
 * @param <T> the type of object this registry manages (e.g. {@code BlockType}, {@code ItemType})
 */
public interface ComponentRegistry<T> extends Registry<T> {

    <H> void registerComponent(ComponentType<H> type, H defaultComponent);

    <H> H getDefaultComponent(ComponentType<H> type);

    ComponentMap getComponents(T type);

    default <H> H getComponent(T type, ComponentType<H> componentType) {
        ComponentMap map = getComponents(type);
        return map == null ? null : map.get(componentType);
    }
}
