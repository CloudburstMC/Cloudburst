package org.cloudburstmc.api.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.util.component.ComponentMap;

/**
 * Provides behavioral components for registered types.
 *
 * @param <T> registered type
 */
public interface ComponentRegistry<T> extends Registry {

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

    /**
     * Returns the component map for a type required to be registered by the caller.
     *
     * @param type registered type
     * @return resolved component map
     * @throws IllegalStateException if the type is not registered
     */
    default ComponentMap requireComponents(T type) {
        ComponentMap map = getComponents(type);
        if (map == null) {
            throw new IllegalStateException("Components are not available for unregistered type: " + type);
        }
        return map;
    }

    default <H> @Nullable H getComponent(T type, ComponentType<H> componentType) {
        ComponentMap map = getComponents(type);
        return map == null ? null : map.get(componentType);
    }

    /**
     * Returns a component that the registry contract requires for the supplied type.
     *
     * @param type registered type
     * @param componentType component type
     * @param <H> component value type
     * @return resolved component
     * @throws IllegalStateException if the registered type does not provide the component
     */
    default <H> H requireComponent(T type, ComponentType<H> componentType) {
        return requireComponents(type).require(componentType);
    }
}
