package org.cloudburstmc.api.block.component;

/**
 * Type-level handler that returns a {@code boolean} value.
 * Used for per-type block properties such as liquid, replaceable, flammable, etc.
 */
@FunctionalInterface
public interface BooleanTypeHandler {

    /**
     * Gets the boolean value for the component.
     *
     * @return the component value
     */
    boolean get();
}
