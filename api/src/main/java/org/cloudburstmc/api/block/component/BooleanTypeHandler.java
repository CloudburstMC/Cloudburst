package org.cloudburstmc.api.block.component;

/**
 * Provides a boolean property of a block type.
 */
@FunctionalInterface
public interface BooleanTypeHandler {

    /**
     * @return the property value
     */
    boolean get();
}
