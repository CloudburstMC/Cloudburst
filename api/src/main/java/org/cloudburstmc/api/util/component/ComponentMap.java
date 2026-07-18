package org.cloudburstmc.api.util.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.data.ComponentType;

/**
 * A read-only collection of behavioral components keyed by {@link ComponentType}.
 */
public interface ComponentMap {

    /**
     * Returns the value for the given component type, or {@code null} if not present.
     *
     * @param type component type
     * @param <H>  component value type
     * @return component value, or {@code null}
     */
    <H> @Nullable H get(ComponentType<H> type);

    /**
     * Returns a component required by the caller's contract.
     *
     * @param type component type
     * @param <H> component value type
     * @return component value
     * @throws IllegalStateException if the component is not present
     */
    default <H> H require(ComponentType<H> type) {
        H component = this.get(type);
        if (component == null) {
            throw new IllegalStateException("Required component is not available: " + type.getId());
        }
        return component;
    }
}
