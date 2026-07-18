package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

/**
 * Obtains a value from a block.
 *
 * @param <T> the returned value type
 */
@FunctionalInterface
public interface GenericBlockHandler<T> {

    /**
     * @param block the block to query
     * @return the component value
     */
    T execute(Block block);
}
