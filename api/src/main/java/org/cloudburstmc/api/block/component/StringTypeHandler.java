package org.cloudburstmc.api.block.component;

/**
 * Provides a string property of a block type.
 */
@FunctionalInterface
public interface StringTypeHandler {

    /**
     * @return the property value
     */
    String get();
}
