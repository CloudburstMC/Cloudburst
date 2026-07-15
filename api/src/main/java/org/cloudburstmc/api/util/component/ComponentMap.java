package org.cloudburstmc.api.util.component;

import org.cloudburstmc.api.data.ComponentType;

/**
 * A flat map of behavioral components keyed by {@link ComponentType}.
 * <p>
 * Each slot holds exactly one handler set via {@link #set}.
 */
public interface ComponentMap {

    /**
     * Returns the handler for the given type, or {@code null} if not present.
     *
     * @param type component type
     * @param <H>  handler type
     * @return handler instance, or {@code null}
     */
    <H> H get(ComponentType<H> type);

    /**
     * Sets the handler for {@code type}, replacing any previous value.
     *
     * @param type    component type
     * @param handler handler to store
     * @param <H>     handler type
     * @return this map (for chaining)
     */
    <H> ComponentMap set(ComponentType<H> type, H handler);
}
