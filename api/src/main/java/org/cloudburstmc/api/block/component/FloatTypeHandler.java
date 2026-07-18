package org.cloudburstmc.api.block.component;

/**
 * Provides a floating-point property of a block type.
 */
@FunctionalInterface
public interface FloatTypeHandler {

    /**
     * @return the property value
     */
    float get();
}
