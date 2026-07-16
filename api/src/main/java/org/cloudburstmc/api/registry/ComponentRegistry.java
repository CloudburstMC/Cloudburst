package org.cloudburstmc.api.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.util.component.ComponentMap;

/**
 * Provides behavioral components for registered types.
 *
 * @param <T> registered type
 */
public interface ComponentRegistry<T> extends Registry<T> {

    /**
     * Registers an optional component slot.
     *
     * @param type component type
     * @param <H> component implementation type
     * @throws IllegalStateException if registration is closed or the identifier is already registered
     */
    <H> void registerComponent(ComponentType<H> type);

    /**
     * Registers a component slot and its default implementation.
     *
     * @param type component type
     * @param defaultComponent implementation used when a registered type has no override
     * @param <H> component implementation type
     * @throws IllegalStateException if registration is closed or the identifier is already registered
     * @throws IllegalArgumentException if the default does not implement the component type
     */
    <H> void registerComponent(ComponentType<H> type, H defaultComponent);

    /**
     * @return the default implementation, or {@code null} when the component has no default
     */
    <H> @Nullable H getDefaultComponent(ComponentType<H> type);

    /**
     * @return the resolved components for a registered type, or {@code null} when the type is unknown
     */
    @Nullable
    ComponentMap getComponents(T type);

    default <H> @Nullable H getComponent(T type, ComponentType<H> componentType) {
        ComponentMap map = getComponents(type);
        return map == null ? null : map.get(componentType);
    }
}
