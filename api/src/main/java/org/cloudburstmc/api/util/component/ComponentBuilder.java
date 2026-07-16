package org.cloudburstmc.api.util.component;

import org.cloudburstmc.api.data.ComponentType;

import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * Configures components while a type is being registered.
 *
 * <p>The builder becomes immutable when its owning registry is closed.
 */
public interface ComponentBuilder extends ComponentMap {

    /**
     * Sets a component, replacing the current value for the same type.
     *
     * @param type component type
     * @param component component implementation
     * @param <H> component type
     * @return this builder
     * @throws IllegalStateException if registration has finished
     * @throws IllegalArgumentException if the component type is not registered or the implementation has the wrong type
     */
    <H> ComponentBuilder set(ComponentType<H> type, H component);

    /**
     * Replaces a component using its currently resolved implementation.
     *
     * @param type component type
     * @param updater function that receives the current implementation
     * @param <H> component implementation type
     * @return this builder
     * @throws IllegalArgumentException if the component type is not registered
     * @throws IllegalStateException if registration has finished
     */
    default <H> ComponentBuilder update(ComponentType<H> type, UnaryOperator<H> updater) {
        requireNonNull(type, "type");
        requireNonNull(updater, "updater");

        H current = get(type);
        if (current == null) {
            throw new IllegalArgumentException("Component is not registered: " + type.getId());
        }

        return set(type, requireNonNull(updater.apply(current), "updated component"));
    }
}
