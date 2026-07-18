package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Obtains a value from an item.
 *
 * @param <T> the returned value type
 */
@FunctionalInterface
public interface GetItemHandler<T> {

    /**
     * @param itemStack the item to query
     * @return the component value
     */
    T execute(ItemStack itemStack);
}
