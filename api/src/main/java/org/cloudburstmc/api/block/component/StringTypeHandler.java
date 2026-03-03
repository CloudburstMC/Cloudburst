package org.cloudburstmc.api.block.component;

/**
 * Type-level handler that returns a {@link String} value.
 * Used for per-type block properties such as map color.
 */
@FunctionalInterface
public interface StringTypeHandler {
    String get();
}
