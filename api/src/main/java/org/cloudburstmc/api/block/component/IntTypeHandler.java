package org.cloudburstmc.api.block.component;

/**
 * Type-level handler that returns an {@code int} value.
 * Used for per-type block properties such as burnOdds, flameOdds, lightDampening, lightEmission, tickDelay, etc.
 */
@FunctionalInterface
public interface IntTypeHandler {
    int get();
}
