package org.cloudburstmc.api.block.component;

/**
 * Provides an integer property of a block type.
 */
@FunctionalInterface
public interface IntTypeHandler {

    /**
     * @return the property value
     */
    int get();
}
