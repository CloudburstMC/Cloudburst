package org.cloudburstmc.api.block.component;

/**
 * Type-level handler that returns a {@code boolean} value.
 * Used for per-type block properties such as solid, liquid, replaceable, flammable, etc.
 */
@FunctionalInterface
public interface BooleanTypeHandler {
    boolean get();
}
