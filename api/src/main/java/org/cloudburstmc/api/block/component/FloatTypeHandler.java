package org.cloudburstmc.api.block.component;

/**
 * Type-level handler that returns a {@code float} value.
 * Used for per-type block properties such as hardness, friction, resistance, etc.
 */
@FunctionalInterface
public interface FloatTypeHandler {
    float get();
}
